package com.baseta.blobstore.module;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModuleView {

    private final Long id;
    private final String code;
    private final String displayName;
    private final ModuleType type;
    private final VideoType videoType;
    private final String storageFolder;
    private final boolean publicAccess;
    private final Integer maxFileSizeMb;
    private final List<ImageSizeView> imageSizes;
    private final ImageSizeView originalImageSize;
    private final List<String> supportedMediaTypes;
    private final Integer maxVideoDurationSeconds;

    public static ModuleView fromEntity(ModuleEntity entity) {
        return new ModuleView(
                entity.getId(),
                entity.getCode(),
                entity.getDisplayName(),
                entity.getType(),
                entity.getVideoType(),
                entity.getStorageFolder(),
                entity.isPublicAccess(),
                entity.getMaxFileSizeMb(),
                entity.getImageSizes().stream().map(ImageSizeView::fromEntity).toList(),
                entity.getOriginalImageWidth() == null || entity.getOriginalImageHeight() == null
                        ? null
                        : new ImageSizeView("original", entity.getOriginalImageWidth(), entity.getOriginalImageHeight()),
                List.copyOf(entity.getSupportedMediaTypes()),
                entity.getMaxVideoDurationSeconds()
        );
    }
}
