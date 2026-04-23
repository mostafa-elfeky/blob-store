package com.baseta.blobstore.module;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModuleForm {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String displayName;

    @NotNull
    private Long projectId;

    @NotNull
    private ModuleType type;

    private VideoType videoType;

    private boolean publicAccess;

    @Min(1)
    private Integer maxFileSizeMb;

    @Size(max = 4000)
    private String imageSizeDefinitions;

    @Size(max = 50)
    private String originalImageSizeDefinition;

    private List<String> selectedMediaTypes = new ArrayList<>();

    @Size(max = 2000)
    private String customMediaTypes;

    @Min(1)
    private Integer maxVideoDurationSeconds;
}
