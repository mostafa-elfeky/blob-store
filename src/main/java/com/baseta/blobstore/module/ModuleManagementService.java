package com.baseta.blobstore.module;

import com.baseta.blobstore.file.FileStorageService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModuleManagementService {

    private final ModuleService moduleService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ModuleEntity save(ModuleForm form) {
        if (form.getId() == null) {
            ModuleEntity created = moduleService.create(form);
            log.info("Created module '{}' with type {}", created.getCode(), created.getType());
            return created;
        }

        ModuleEntity existing = moduleService.getById(form.getId());
        List<ImageSizeDefinition> previousImageSizes = copyImageSizes(existing.getImageSizes());
        ModuleEntity updated = moduleService.update(existing.getId(), form);
        fileStorageService.syncImageModuleVariants(updated, previousImageSizes);
        log.info("Updated module '{}'", updated.getCode());
        return updated;
    }

    @Transactional
    public void delete(Long moduleId) {
        ModuleEntity module = moduleService.getById(moduleId);
        fileStorageService.deleteGeneratedArtifacts(module, copyImageSizes(module.getImageSizes()));
        moduleService.softDelete(moduleId);
        log.info("Soft-deleted module '{}'", module.getCode());
    }

    private List<ImageSizeDefinition> copyImageSizes(List<ImageSizeDefinition> imageSizes) {
        return imageSizes.stream()
                .map(size -> new ImageSizeDefinition(size.getCode(), size.getWidth(), size.getHeight()))
                .toList();
    }
}
