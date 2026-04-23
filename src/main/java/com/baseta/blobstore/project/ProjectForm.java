package com.baseta.blobstore.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectForm {

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String displayName;
}
