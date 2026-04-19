package com.baseta.blobstore.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StorageSettingsForm {

    @NotBlank
    @Size(max = 1000)
    private String rootDir;
}
