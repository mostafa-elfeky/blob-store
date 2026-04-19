package com.baseta.blobstore.module;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageSizeView {

    private final String code;
    private final Integer width;
    private final Integer height;

    public static ImageSizeView fromEntity(ImageSizeDefinition definition) {
        return new ImageSizeView(definition.getCode(), definition.getWidth(), definition.getHeight());
    }
}
