package com.baseta.blobstore.module;

import com.baseta.blobstore.storage.StorageSettingsService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final StorageSettingsService storageSettingsService;
    private final ImageSizeDefinitionParser imageSizeDefinitionParser;

    public List<ModuleView> findAll() {
        return moduleRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(ModuleView::fromEntity)
                .toList();
    }

    public List<ModuleEntity> findAllEntities() {
        return moduleRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    public ModuleForm buildForm(Long moduleId) {
        ModuleEntity module = getById(moduleId);
        ModuleForm form = new ModuleForm();
        form.setId(module.getId());
        form.setCode(module.getCode());
        form.setDisplayName(module.getDisplayName());
        form.setType(module.getType());
        form.setVideoType(module.getVideoType());
        form.setPublicAccess(module.isPublicAccess());
        form.setMaxFileSizeMb(module.getMaxFileSizeMb());
        form.setImageSizeDefinitions(module.getImageSizes().stream()
                .map(size -> size.getCode() + "=" + size.getWidth() + "x" + size.getHeight())
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null));
        form.setOriginalImageSizeDefinition(module.getOriginalImageWidth() == null || module.getOriginalImageHeight() == null
                ? null
                : module.getOriginalImageWidth() + "x" + module.getOriginalImageHeight());
        form.setSelectedMediaTypes(module.getSupportedMediaTypes().stream()
                .filter(ModuleMediaTypeSupport::isPreset)
                .toList());
        form.setCustomMediaTypes(ModuleMediaTypeSupport.joinCustomMediaTypes(module.getSupportedMediaTypes()));
        form.setMaxVideoDurationSeconds(module.getMaxVideoDurationSeconds());
        return form;
    }

    public ModuleEntity getByCode(String code) {
        return moduleRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code)
                .orElseThrow(() -> new ModuleNotFoundException(code));
    }

    public ModuleEntity getById(Long id) {
        return moduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ModuleNotFoundException(String.valueOf(id)));
    }

    @Transactional
    public ModuleEntity create(ModuleForm form) {
        String moduleCode = ModuleCodeNormalizer.normalize(form.getCode(), "Module code");
        if (moduleRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(moduleCode)) {
            throw new IllegalArgumentException("Module code already exists");
        }

        ModuleEntity module = new ModuleEntity();
        applyCreationFields(module, form, moduleCode);
        ensureModuleDirectory(moduleCode);
        return moduleRepository.save(module);
    }

    @Transactional
    public ModuleEntity update(Long id, ModuleForm form) {
        ModuleEntity module = getById(id);
        String moduleCode = ModuleCodeNormalizer.normalize(form.getCode(), "Module code");
        if (!module.getCode().equals(moduleCode)) {
            throw new IllegalArgumentException("Module code cannot be changed");
        }
        if (module.getType() != form.getType()) {
            throw new IllegalArgumentException("Module type cannot be changed");
        }

        module.setDisplayName(form.getDisplayName().trim());
        module.setVideoType(resolveVideoType(form));
        module.setPublicAccess(form.isPublicAccess());
        module.setMaxFileSizeMb(form.getMaxFileSizeMb());
        module.setImageSizes(resolveImageSizes(form));
        applyOriginalImageSize(module, form);
        module.setSupportedMediaTypes(resolveSupportedMediaTypes(form));
        module.setMaxVideoDurationSeconds(form.getType() == ModuleType.VIDEO ? form.getMaxVideoDurationSeconds() : null);
        return moduleRepository.save(module);
    }

    @Transactional
    public ModuleEntity softDelete(Long id) {
        ModuleEntity module = getById(id);
        module.setDeletedAt(Instant.now());
        module.setImageSizes(new ArrayList<>());
        return moduleRepository.save(module);
    }

    private void applyCreationFields(ModuleEntity module, ModuleForm form, String moduleCode) {
        module.setCode(moduleCode);
        module.setDisplayName(form.getDisplayName().trim());
        module.setType(form.getType());
        module.setVideoType(resolveVideoType(form));
        module.setPublicAccess(form.isPublicAccess());
        module.setMaxFileSizeMb(form.getMaxFileSizeMb());
        module.setImageSizes(resolveImageSizes(form));
        applyOriginalImageSize(module, form);
        module.setSupportedMediaTypes(resolveSupportedMediaTypes(form));
        module.setMaxVideoDurationSeconds(form.getType() == ModuleType.VIDEO ? form.getMaxVideoDurationSeconds() : null);
        module.setStorageFolder(moduleCode);
    }

    public Path resolveModuleDirectory(ModuleEntity module) {
        return storageSettingsService.currentRootPath().resolve(module.getStorageFolder()).normalize();
    }

    private void ensureModuleDirectory(String folderName) {
        try {
            Files.createDirectories(storageSettingsService.currentRootPath().resolve(folderName));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create module folder", exception);
        }
    }

    private VideoType resolveVideoType(ModuleForm form) {
        if (form.getType() != ModuleType.VIDEO) {
            return null;
        }
        if (form.getVideoType() == null) {
            throw new IllegalArgumentException("Video type is required for video modules");
        }
        return form.getVideoType();
    }

    private List<ImageSizeDefinition> resolveImageSizes(ModuleForm form) {
        if (form.getType() != ModuleType.IMAGE) {
            return List.of();
        }
        return imageSizeDefinitionParser.parse(form.getImageSizeDefinitions());
    }

    private void applyOriginalImageSize(ModuleEntity module, ModuleForm form) {
        if (form.getType() != ModuleType.IMAGE) {
            module.setOriginalImageWidth(null);
            module.setOriginalImageHeight(null);
            return;
        }

        ImageDimensions originalImageSize = imageSizeDefinitionParser.parseOptionalOriginalSize(form.getOriginalImageSizeDefinition());
        if (originalImageSize == null) {
            module.setOriginalImageWidth(null);
            module.setOriginalImageHeight(null);
            return;
        }

        module.setOriginalImageWidth(originalImageSize.width());
        module.setOriginalImageHeight(originalImageSize.height());
    }

    private List<String> resolveSupportedMediaTypes(ModuleForm form) {
        return resolveSupportedMediaTypes(form.getType(), form.getSelectedMediaTypes(), form.getCustomMediaTypes());
    }

    private List<String> resolveSupportedMediaTypes(
            ModuleType moduleType,
            List<String> selectedMediaTypes,
            String customMediaTypes
    ) {
        List<String> supportedMediaTypes = ModuleMediaTypeSupport.resolveAllowedMediaTypes(selectedMediaTypes, customMediaTypes);
        ModuleMediaTypeSupport.validateSupportedMediaTypes(moduleType, supportedMediaTypes);
        return supportedMediaTypes;
    }
}
