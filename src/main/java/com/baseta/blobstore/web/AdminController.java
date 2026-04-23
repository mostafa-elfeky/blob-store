package com.baseta.blobstore.web;

import com.baseta.blobstore.database.DatabaseConnectionForm;
import com.baseta.blobstore.database.DatabaseConnectionService;
import com.baseta.blobstore.database.DatabaseVendor;
import com.baseta.blobstore.file.FileStorageService;
import com.baseta.blobstore.logging.InMemoryLogStore;
import com.baseta.blobstore.module.ModuleForm;
import com.baseta.blobstore.module.ModuleManagementService;
import com.baseta.blobstore.module.ModuleMediaTypeSupport;
import com.baseta.blobstore.module.ModuleService;
import com.baseta.blobstore.module.ModuleType;
import com.baseta.blobstore.project.ProjectForm;
import com.baseta.blobstore.project.ProjectService;
import com.baseta.blobstore.module.VideoType;
import com.baseta.blobstore.storage.StorageSettingsForm;
import com.baseta.blobstore.storage.StorageSettingsService;
import jakarta.validation.Valid;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final DatabaseConnectionService databaseConnectionService;
    private final ModuleManagementService moduleManagementService;
    private final ModuleService moduleService;
    private final FileStorageService fileStorageService;
    private final InMemoryLogStore logStore;
    private final StorageSettingsService storageSettingsService;
    private final ProjectService projectService;

    @GetMapping({"/", "/admin"})
    public String dashboard(
            @RequestParam(required = false) Long editModuleId,
            Model model
    ) {
        ensureDashboardForms(model, editModuleId);
        populateDashboardModel(model);
        return "admin/dashboard";
    }

    @PostMapping("/admin/modules")
    public String saveModule(
            @Valid @ModelAttribute("moduleForm") ModuleForm moduleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeTab", "modules");
            ensureDashboardForms(model, moduleForm.getId());
            populateDashboardModel(model);
            return "admin/dashboard";
        }

        try {
            boolean editing = moduleForm.getId() != null;
            moduleManagementService.save(moduleForm);
            redirectAttributes.addFlashAttribute("successMessage", editing ? "Module updated successfully" : "Module created successfully");
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("module.form", exception.getMessage());
            model.addAttribute("activeTab", "modules");
            ensureDashboardForms(model, moduleForm.getId());
            populateDashboardModel(model);
            return "admin/dashboard";
        }

        return "redirect:/admin#modules";
    }

    @PostMapping("/admin/projects")
    public String saveProject(
            @Valid @ModelAttribute("projectForm") ProjectForm projectForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projectDialogOpen", true);
            model.addAttribute("activeTab", "modules");
            ensureDashboardForms(model, null);
            populateDashboardModel(model);
            return "admin/dashboard";
        }

        try {
            projectService.create(projectForm);
            redirectAttributes.addFlashAttribute("projectSuccessMessage", "Project created successfully");
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("project.form", exception.getMessage());
            model.addAttribute("projectDialogOpen", true);
            model.addAttribute("activeTab", "modules");
            ensureDashboardForms(model, null);
            populateDashboardModel(model);
            return "admin/dashboard";
        }

        redirectAttributes.addFlashAttribute("activeTab", "modules");
        return "redirect:/admin#modules";
    }

    @PostMapping("/admin/database/test")
    public String testDatabaseConnection(
            @Valid @ModelAttribute("databaseForm") DatabaseConnectionForm databaseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return renderDatabaseErrorState(model);
        }

        try {
            databaseConnectionService.test(databaseForm);
            redirectAttributes.addFlashAttribute("databaseSuccessMessage", "Database connection test succeeded");
        } catch (IllegalArgumentException exception) {
            return renderDatabaseConnectionFailure(model, exception);
        }

        redirectAttributes.addFlashAttribute("databaseForm", databaseForm);
        redirectAttributes.addFlashAttribute("activeTab", "database");
        return "redirect:/admin#database";
    }

    @PostMapping("/admin/database")
    public String saveDatabaseConnection(
            @Valid @ModelAttribute("databaseForm") DatabaseConnectionForm databaseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return renderDatabaseErrorState(model);
        }

        try {
            databaseConnectionService.test(databaseForm);
            databaseConnectionService.save(databaseForm);
            redirectAttributes.addFlashAttribute(
                    "databaseSuccessMessage",
                    "Database settings saved. Restart the application to apply the new connection."
            );
            redirectAttributes.addFlashAttribute("databaseForm", databaseForm);
        } catch (IllegalArgumentException exception) {
            return renderDatabaseConnectionFailure(model, exception);
        }

        redirectAttributes.addFlashAttribute("activeTab", "database");
        return "redirect:/admin#database";
    }

    @PostMapping("/admin/modules/{id}/delete")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        moduleManagementService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Module deleted. Original files were kept on disk.");
        return "redirect:/admin#modules";
    }

    @PostMapping("/admin/files/purge-deleted")
    public String purgeDeletedFiles(RedirectAttributes redirectAttributes) {
        int deletedCount = fileStorageService.permanentlyDeleteFilesMarkedDeleted();
        redirectAttributes.addFlashAttribute(
                "settingsSuccessMessage",
                deletedCount == 0
                        ? "No deleted files were waiting for permanent removal."
                        : "Permanently deleted " + deletedCount + " file" + (deletedCount == 1 ? "" : "s") + "."
        );
        redirectAttributes.addFlashAttribute("activeTab", "settings");
        return "redirect:/admin#settings";
    }

    @PostMapping("/admin/storage-settings")
    public String saveStorageSettings(
            @Valid @ModelAttribute("storageSettingsForm") StorageSettingsForm storageSettingsForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return renderSettingsErrorState(model);
        }

        try {
            StorageSettingsService.SaveResult result = storageSettingsService.save(storageSettingsForm);
            redirectAttributes.addFlashAttribute(
                    "settingsSuccessMessage",
                    result == StorageSettingsService.SaveResult.UNCHANGED
                            ? "Storage root is unchanged."
                            : "Storage root saved. Restart the application to apply the new location."
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("storage.form", exception.getMessage());
            return renderSettingsErrorState(model);
        }
        redirectAttributes.addFlashAttribute("storageSettingsForm", storageSettingsForm);
        redirectAttributes.addFlashAttribute("activeTab", "settings");
        return "redirect:/admin#settings";
    }

    private void populateDashboardModel(Model model) {
        var databaseStatus = databaseConnectionService.currentStatus();
        model.addAttribute("databaseStatus", databaseStatus);
        model.addAttribute("storageSettingsStatus", storageSettingsService.currentStatus());
        model.addAttribute("databaseVendors", Arrays.stream(DatabaseVendor.values()).filter(vendor -> vendor != DatabaseVendor.H2).toList());
        model.addAttribute("moduleTypes", ModuleType.values());
        model.addAttribute("videoTypes", VideoType.values());
        model.addAttribute("mediaTypeOptions", ModuleMediaTypeSupport.presetMediaTypes());
        if (databaseStatus.isDatabaseReady()) {
            projectService.ensureDefaultProject();
            model.addAttribute("projects", projectService.findAll());
        } else {
            model.addAttribute("projects", java.util.List.of());
        }
        model.addAttribute("modules", databaseStatus.isDatabaseReady() ? moduleService.findAll() : java.util.List.of());
        model.addAttribute("recentFiles", databaseStatus.isDatabaseReady() ? fileStorageService.findRecent() : java.util.List.of());
        model.addAttribute("deletedFileCount", databaseStatus.isDatabaseReady() ? fileStorageService.countFilesMarkedDeleted() : 0L);
        model.addAttribute("systemLogs", logStore.recentEntries());
    }

    private String renderDatabaseErrorState(Model model) {
        model.addAttribute("databaseFormOpen", true);
        model.addAttribute("activeTab", "database");
        ensureDashboardForms(model, null);
        populateDashboardModel(model);
        return "admin/dashboard";
    }

    private String renderSettingsErrorState(Model model) {
        model.addAttribute("activeTab", "settings");
        ensureDashboardForms(model, null);
        populateDashboardModel(model);
        return "admin/dashboard";
    }

    private String renderDatabaseConnectionFailure(Model model, IllegalArgumentException exception) {
        model.addAttribute("databaseErrorMessage", "Not able to connect to this database.");
        model.addAttribute("databaseErrorDetail", exception.getMessage());
        return renderDatabaseErrorState(model);
    }

    private void ensureDashboardForms(Model model, Long editModuleId) {
        if (!model.containsAttribute("moduleForm")) {
            ModuleForm moduleForm = editModuleId == null ? new ModuleForm() : moduleService.buildForm(editModuleId);
            if (editModuleId == null && moduleForm.getProjectId() == null && databaseConnectionService.currentStatus().isDatabaseReady()) {
                moduleForm.setProjectId(projectService.defaultProjectId());
            }
            model.addAttribute("moduleForm", moduleForm);
        }
        if (!model.containsAttribute("databaseForm")) {
            model.addAttribute("databaseForm", databaseConnectionService.currentForm());
        }
        if (!model.containsAttribute("storageSettingsForm")) {
            model.addAttribute("storageSettingsForm", storageSettingsService.currentForm());
        }
        if (!model.containsAttribute("projectForm")) {
            model.addAttribute("projectForm", new ProjectForm());
        }
        if (!model.containsAttribute("databaseFormOpen")) {
            model.addAttribute("databaseFormOpen", false);
        }
        if (!model.containsAttribute("projectDialogOpen")) {
            model.addAttribute("projectDialogOpen", false);
        }
    }
}
