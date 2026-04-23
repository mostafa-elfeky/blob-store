package com.baseta.blobstore.web;

import com.baseta.blobstore.module.ModuleEntity;
import com.baseta.blobstore.module.ModuleForm;
import com.baseta.blobstore.module.ModuleService;
import com.baseta.blobstore.module.ModuleView;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleApiController {

    private final ModuleService moduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_blobstore.modules.read')")
    public List<ModuleView> listModules() {
        return moduleService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_blobstore.modules.write')")
    public ModuleView createModule(@Valid @RequestBody ModuleForm form) {
        ModuleEntity module = moduleService.create(form);
        return ModuleView.fromEntity(module);
    }
}
