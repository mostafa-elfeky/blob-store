package com.baseta.blobstore.module;

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
public class ModuleMediaSettingsForm {

    @NotNull
    private Long moduleId;

    private List<String> selectedMediaTypes = new ArrayList<>();

    @Size(max = 2000)
    private String customMediaTypes;
}
