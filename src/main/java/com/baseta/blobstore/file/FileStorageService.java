package com.baseta.blobstore.file;

import com.baseta.blobstore.module.ImageSizeDefinition;
import com.baseta.blobstore.module.ModuleEntity;
import com.baseta.blobstore.module.ModuleMediaTypeSupport;
import com.baseta.blobstore.module.ModuleService;
import com.baseta.blobstore.module.ModuleType;
import jakarta.transaction.Transactional;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private static final List<String> IMAGE_PREFIXES = List.of("image/");
    private static final List<String> VIDEO_PREFIXES = List.of("video/");

    private final ModuleService moduleService;
    private final StoredFileRepository storedFileRepository;

    @Transactional
    public StoredFileView store(String moduleCode, MultipartFile multipartFile) {
        ModuleEntity module = moduleService.getByCode(moduleCode);
        validateFile(module, multipartFile);

        String originalName = StringUtils.hasText(multipartFile.getOriginalFilename())
                ? Path.of(multipartFile.getOriginalFilename()).getFileName().toString()
                : "file";
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        String retrievalName = storedName;
        Path targetPath = resolveOriginalFilePath(module, storedName);
        ensureParentDirectory(targetPath);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }

        if (module.getType() == ModuleType.IMAGE) {
            generateImageVariants(module, targetPath, retrievalName, resolveContentType(multipartFile));
        }

        StoredFileEntity entity = buildStoredFile(module, originalName, storedName, retrievalName, multipartFile);
        StoredFileView storedFileView = StoredFileView.fromEntity(storedFileRepository.save(entity));
        log.info("Stored file '{}' in module '{}'", originalName, module.getCode());
        return storedFileView;
    }

    public FilePayload open(UUID fileKey) {
        return open(fileKey, null);
    }

    public FilePayload open(UUID fileKey, String type) {
        StoredFileEntity file = storedFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new StoredFileNotFoundException(fileKey));
        Path sourcePath = resolveFilePathByType(file, type);
        return openFilePayload(file, sourcePath);
    }

    public StoredFileView getMetadata(UUID fileKey) {
        StoredFileEntity file = storedFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new StoredFileNotFoundException(fileKey));
        return StoredFileView.fromEntity(file);
    }

    public List<StoredFileView> findRecent() {
        return storedFileRepository.findTop20ByModuleDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(StoredFileView::fromEntity)
                .toList();
    }

    public long countFilesMarkedDeleted() {
        return storedFileRepository.countByModuleDeletedAtIsNotNull();
    }

    @Transactional
    public int permanentlyDeleteFilesMarkedDeleted() {
        List<StoredFileEntity> deletedFiles = storedFileRepository.findAllByModuleDeletedAtIsNotNull();
        deletedFiles.forEach(this::deleteStoredArtifacts);
        storedFileRepository.deleteAllInBatch(deletedFiles);
        log.info("Permanently deleted {} file(s) for soft-deleted modules", deletedFiles.size());
        return deletedFiles.size();
    }

    public void syncImageModuleVariants(ModuleEntity module, List<ImageSizeDefinition> previousImageSizes) {
        if (module.getType() != ModuleType.IMAGE) {
            return;
        }

        Map<String, ImageSizeDefinition> previousByCode = previousImageSizes.stream()
                .collect(Collectors.toMap(ImageSizeDefinition::getCode, Function.identity()));
        Map<String, ImageSizeDefinition> currentByCode = module.getImageSizes().stream()
                .collect(Collectors.toMap(ImageSizeDefinition::getCode, Function.identity()));

        previousByCode.keySet().stream()
                .filter(code -> !currentByCode.containsKey(code))
                .forEach(code -> {
                    deleteDirectory(resolveImageSizeDirectory(module, code));
                    deleteLegacyImageVariants(module, code);
                });

        module.getImageSizes().forEach(size -> {
            ImageSizeDefinition previous = previousByCode.get(size.getCode());
            if (previous == null || !sameDimensions(previous, size)) {
                regenerateImageSize(module, size);
            }
        });
        log.info("Synchronized image variants for module '{}'", module.getCode());
    }

    public void deleteGeneratedArtifacts(ModuleEntity module, List<ImageSizeDefinition> imageSizes) {
        if (module.getType() != ModuleType.IMAGE) {
            return;
        }

        imageSizes.forEach(size -> {
            deleteDirectory(resolveImageSizeDirectory(module, size.getCode()));
            deleteLegacyImageVariants(module, size.getCode());
        });
    }

    private void validateFile(ModuleEntity module, MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (module.getMaxFileSizeMb() != null && multipartFile.getSize() > module.getMaxFileSizeMb() * 1024L * 1024L) {
            throw new IllegalArgumentException("Uploaded file exceeds module size limit");
        }

        String contentType = resolveContentType(multipartFile).toLowerCase(Locale.ROOT);
        if (!module.getSupportedMediaTypes().isEmpty()) {
            if (!ModuleMediaTypeSupport.matches(contentType, module.getSupportedMediaTypes())) {
                throw new IllegalArgumentException("Uploaded file media type is not allowed");
            }
        } else {
            if (module.getType() == ModuleType.IMAGE && IMAGE_PREFIXES.stream().noneMatch(contentType::startsWith)) {
                throw new IllegalArgumentException("Module accepts images only");
            }
            if (module.getType() == ModuleType.VIDEO && VIDEO_PREFIXES.stream().noneMatch(contentType::startsWith)) {
                throw new IllegalArgumentException("Module accepts videos only");
            }
        }
        validateOriginalImageSize(module, multipartFile);
    }

    private void validateOriginalImageSize(ModuleEntity module, MultipartFile multipartFile) {
        if (module.getType() != ModuleType.IMAGE
                || module.getOriginalImageWidth() == null
                || module.getOriginalImageHeight() == null) {
            return;
        }

        BufferedImage image;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            image = ImageIO.read(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read uploaded image", exception);
        }
        if (image == null) {
            throw new IllegalArgumentException("Unsupported image format");
        }
        if (image.getWidth() != module.getOriginalImageWidth() || image.getHeight() != module.getOriginalImageHeight()) {
            throw new IllegalArgumentException(
                    "Uploaded image must match the original size "
                            + module.getOriginalImageWidth()
                            + "x"
                            + module.getOriginalImageHeight()
            );
        }
    }

    private String resolveContentType(MultipartFile multipartFile) {
        return StringUtils.hasText(multipartFile.getContentType())
                ? multipartFile.getContentType()
                : "application/octet-stream";
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index) : "";
    }

    private StoredFileEntity buildStoredFile(
            ModuleEntity module,
            String originalName,
            String storedName,
            String retrievalName,
            MultipartFile multipartFile
    ) {
        StoredFileEntity entity = new StoredFileEntity();
        entity.setFileKey(UUID.randomUUID());
        entity.setModule(module);
        entity.setOriginalName(originalName);
        entity.setStoredName(storedName);
        entity.setRetrievalName(retrievalName);
        entity.setContentType(resolveContentType(multipartFile));
        entity.setFileSize(multipartFile.getSize());
        return entity;
    }

    private void deleteStoredArtifacts(StoredFileEntity file) {
        deleteFileIfExists(resolveStoredFilePath(file));
        if (file.getModule().getType() == ModuleType.IMAGE) {
            Path moduleDirectory = moduleService.resolveModuleDirectory(file.getModule());
            if (!Files.exists(moduleDirectory)) {
                return;
            }
            try (var directories = Files.list(moduleDirectory)) {
                directories
                        .filter(Files::isDirectory)
                        .forEach(directory -> deleteFileIfExists(directory.resolve(file.getStoredName()).normalize()));
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete stored file variants", exception);
            }
        }
    }

    private Path resolveStoredFilePath(StoredFileEntity file) {
        Path originalPath = resolveOriginalFilePath(file.getModule(), file.getStoredName());
        if (file.getModule().getType() == ModuleType.IMAGE && !Files.exists(originalPath)) {
            return moduleService.resolveModuleDirectory(file.getModule()).resolve(file.getStoredName()).normalize();
        }
        return originalPath;
    }

    private Path resolveFilePathByType(StoredFileEntity file, String type) {
        if (!StringUtils.hasText(type)) {
            return resolveStoredFilePath(file);
        }
        if (file.getModule().getType() != ModuleType.IMAGE) {
            throw new IllegalArgumentException("Type is supported for image modules only");
        }

        String normalizedSizeCode = type.trim().toLowerCase(Locale.ROOT);
        boolean knownSize = file.getModule().getImageSizes().stream()
                .map(ImageSizeDefinition::getCode)
                .anyMatch(normalizedSizeCode::equals);
        if (!knownSize) {
            throw new IllegalArgumentException("Unknown image type: " + normalizedSizeCode);
        }
        Path sizePath = resolveImageSizeDirectory(file.getModule(), normalizedSizeCode).resolve(file.getStoredName()).normalize();
        if (!Files.exists(sizePath)) {
            return moduleService.resolveModuleDirectory(file.getModule())
                    .resolve(buildLegacyVariantName(file.getStoredName(), normalizedSizeCode))
                    .normalize();
        }
        return sizePath;
    }

    private FilePayload openFilePayload(StoredFileEntity file, Path sourcePath) {
        try {
            return new FilePayload(
                    file.getFileKey().toString(),
                    file.getModule().getCode(),
                    file.getOriginalName(),
                    file.getContentType(),
                    Files.size(sourcePath),
                    Files.newInputStream(sourcePath)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read stored file", exception);
        }
    }

    private void generateImageVariants(ModuleEntity module, Path originalPath, String retrievalName, String contentType) {
        BufferedImage sourceImage;
        try {
            sourceImage = ImageIO.read(originalPath.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read image for resizing", exception);
        }
        if (sourceImage == null) {
            throw new IllegalArgumentException("Unsupported image format for resizing");
        }

        String formatName = resolveImageFormat(originalPath.getFileName().toString(), contentType);
        for (ImageSizeDefinition size : module.getImageSizes()) {
            BufferedImage resized = resize(sourceImage, size.getWidth(), size.getHeight());
            Path variantPath = resolveImageSizeDirectory(module, size.getCode()).resolve(retrievalName).normalize();
            ensureParentDirectory(variantPath);
            try (OutputStream outputStream = Files.newOutputStream(variantPath)) {
                if (!ImageIO.write(resized, formatName, outputStream)) {
                    throw new IllegalArgumentException("Unsupported image format for resizing");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to write image variant", exception);
            }
        }
    }

    private Path resolveOriginalFilePath(ModuleEntity module, String storedName) {
        Path moduleDirectory = moduleService.resolveModuleDirectory(module);
        if (module.getType() != ModuleType.IMAGE) {
            return moduleDirectory.resolve(storedName).normalize();
        }
        return moduleDirectory.resolve("original").resolve(storedName).normalize();
    }

    private Path resolveImageSizeDirectory(ModuleEntity module, String sizeCode) {
        return moduleService.resolveModuleDirectory(module).resolve(sizeCode).normalize();
    }

    private void regenerateImageSize(ModuleEntity module, ImageSizeDefinition size) {
        Path targetDirectory = resolveImageSizeDirectory(module, size.getCode());
        deleteDirectory(targetDirectory);
        deleteLegacyImageVariants(module, size.getCode());
        ensureDirectory(targetDirectory);

        for (StoredFileEntity file : storedFileRepository.findAllByModuleId(module.getId())) {
            Path originalPath = resolveStoredFilePath(file);
            if (!Files.exists(originalPath)) {
                continue;
            }
            resizeSingleImage(originalPath, targetDirectory.resolve(file.getStoredName()), size, file.getContentType());
        }
    }

    private void resizeSingleImage(
            Path originalPath,
            Path variantPath,
            ImageSizeDefinition size,
            String contentType
    ) {
        BufferedImage sourceImage;
        try {
            sourceImage = ImageIO.read(originalPath.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read image for resizing", exception);
        }
        if (sourceImage == null) {
            throw new IllegalArgumentException("Unsupported image format for resizing");
        }

        BufferedImage resized = resize(sourceImage, size.getWidth(), size.getHeight());
        String formatName = resolveImageFormat(originalPath.getFileName().toString(), contentType);
        ensureParentDirectory(variantPath);
        try (OutputStream outputStream = Files.newOutputStream(variantPath)) {
            if (!ImageIO.write(resized, formatName, outputStream)) {
                throw new IllegalArgumentException("Unsupported image format for resizing");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write image variant", exception);
        }
    }

    private boolean sameDimensions(ImageSizeDefinition left, ImageSizeDefinition right) {
        return Objects.equals(left.getWidth(), right.getWidth()) && Objects.equals(left.getHeight(), right.getHeight());
    }

    private void ensureParentDirectory(Path path) {
        ensureDirectory(path.getParent());
    }

    private void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create directory", exception);
        }
    }

    private void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("Failed to delete directory", exception);
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete directory", exception);
        }
    }

    private void deleteFileIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete stored file", exception);
        }
    }

    private void deleteLegacyImageVariants(ModuleEntity module, String sizeCode) {
        Path moduleDirectory = moduleService.resolveModuleDirectory(module);
        for (StoredFileEntity file : storedFileRepository.findAllByModuleId(module.getId())) {
            try {
                Files.deleteIfExists(moduleDirectory.resolve(buildLegacyVariantName(file.getStoredName(), sizeCode)).normalize());
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete image variant", exception);
            }
        }
    }

    private String buildLegacyVariantName(String retrievalName, String sizeCode) {
        int dotIndex = retrievalName.lastIndexOf('.');
        if (dotIndex < 0) {
            return retrievalName + "__" + sizeCode;
        }
        return retrievalName.substring(0, dotIndex) + "__" + sizeCode + retrievalName.substring(dotIndex);
    }

    private BufferedImage resize(BufferedImage sourceImage, int maxWidth, int maxHeight) {
        double ratio = Math.min((double) maxWidth / sourceImage.getWidth(), (double) maxHeight / sourceImage.getHeight());
        ratio = Math.min(ratio, 1.0d);
        int targetWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * ratio));
        int targetHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * ratio));
        int imageType = sourceImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private String resolveImageFormat(String fileName, String contentType) {
        String extension = extractExtension(fileName).replace(".", "").toLowerCase(Locale.ROOT);
        if (List.of("jpg", "jpeg", "png", "bmp", "gif").contains(extension)) {
            return Objects.equals(extension, "jpg") ? "jpeg" : extension;
        }
        if (contentType.toLowerCase(Locale.ROOT).contains("png")) {
            return "png";
        }
        if (contentType.toLowerCase(Locale.ROOT).contains("jpeg") || contentType.toLowerCase(Locale.ROOT).contains("jpg")) {
            return "jpeg";
        }
        throw new IllegalArgumentException("Unsupported image format for resizing");
    }

}
