package com.baseta.blobstore.module;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageSizeDefinition {

    @Column(name = "size_code", nullable = false, length = 50)
    private String code;

    @Column(name = "target_width", nullable = false)
    private Integer width;

    @Column(name = "target_height", nullable = false)
    private Integer height;

}
