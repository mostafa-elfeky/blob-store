const dashboardApp = document.getElementById("dashboardApp");
const moduleTypeSelect = document.getElementById("moduleType");
const imageFields = document.getElementById("imageFields");
const videoFields = document.getElementById("videoFields");
const databaseVendorSelect = document.getElementById("databaseVendor");
const databasePortInput = document.getElementById("databasePort");
const databaseFormPanel = document.getElementById("databaseFormPanel");
const showDatabaseFormButton = document.getElementById("showDatabaseFormButton");
const cancelDatabaseFormButton = document.getElementById("cancelDatabaseFormButton");
const moduleMediaTypeOptions = document.querySelectorAll("[data-module-media-group]");
const moduleMediaTypeScopeEyebrow = document.getElementById("moduleMediaTypeScopeEyebrow");
const moduleMediaTypeScopeTitle = document.getElementById("moduleMediaTypeScopeTitle");
const moduleMediaTypeScopeDescription = document.getElementById("moduleMediaTypeScopeDescription");
const moduleMediaTypeVisibleCountValue = document.getElementById("moduleMediaTypeVisibleCountValue");
const moduleMediaTypeScopeFamilyValue = document.getElementById("moduleMediaTypeScopeFamilyValue");
const tabButtons = document.querySelectorAll("[data-tab-target]");
const tabPanels = document.querySelectorAll("[data-tab-panel]");
const copyCurlButtons = document.querySelectorAll("[data-curl-template]");
const projectDialog = document.getElementById("projectDialog");
const projectDialogOpenButtons = document.querySelectorAll("[data-open-project-dialog]");
const projectDialogCloseButtons = document.querySelectorAll("[data-close-project-dialog]");
const storageSettingsForm = document.getElementById("storageSettingsForm");
const storageRootInput = document.getElementById("rootDir");
const storageSavedPathValue = document.getElementById("storageSavedPathValue");
const storageActivationState = document.getElementById("storageActivationState");
const apiSecurityForm = document.getElementById("apiSecurityForm");
const apiJwtValidationModes = document.querySelectorAll("input[name='jwtValidationMode']");
const apiJwtSharedSecretField = document.getElementById("apiJwtSharedSecretField");
const apiJwtJwkSetUriField = document.getElementById("apiJwtJwkSetUriField");
const apiBasicAuthEnabled = document.getElementById("apiBasicAuthEnabled");
const apiBasicAuthFields = document.getElementById("apiBasicAuthFields");
const vendorPorts = {
    POSTGRESQL: 5432,
    MYSQL: 3306,
    SQLSERVER: 1433,
    ORACLE: 1521
};

function parseBoolean(value) {
    return value === "true";
}

function syncModuleMediaSettings(moduleType) {
    let visibleCount = 0;
    moduleMediaTypeOptions.forEach(option => {
        const group = option.dataset.moduleMediaGroup;
        const shouldShow = !moduleType
            || moduleType === "FILE"
            || (moduleType === "IMAGE" && group === "image")
            || (moduleType === "VIDEO" && group === "video");
        option.hidden = !shouldShow;
        const checkbox = option.querySelector("input");
        if (checkbox) {
            checkbox.disabled = !shouldShow;
        }
        if (shouldShow) {
            visibleCount += 1;
        }
    });

    if (moduleMediaTypeVisibleCountValue) {
        moduleMediaTypeVisibleCountValue.textContent = String(visibleCount);
    }
    if (moduleMediaTypeScopeEyebrow && moduleMediaTypeScopeTitle && moduleMediaTypeScopeDescription && moduleMediaTypeScopeFamilyValue) {
        if (!moduleType) {
            moduleMediaTypeScopeEyebrow.textContent = "Upload scope";
            moduleMediaTypeScopeTitle.textContent = "Choose a module type to preview its allowed media families";
            moduleMediaTypeScopeDescription.textContent = "File modules can use any supported media families. Image and video modules are limited to their own families.";
            moduleMediaTypeScopeFamilyValue.textContent = "all";
        } else if (moduleType === "IMAGE") {
            moduleMediaTypeScopeEyebrow.textContent = "Image module";
            moduleMediaTypeScopeTitle.textContent = "Only image media types can be selected here";
            moduleMediaTypeScopeDescription.textContent = "Preset choices are filtered to image/* values so upload validation stays aligned with image processing rules.";
            moduleMediaTypeScopeFamilyValue.textContent = "image/*";
        } else if (moduleType === "VIDEO") {
            moduleMediaTypeScopeEyebrow.textContent = "Video module";
            moduleMediaTypeScopeTitle.textContent = "Only video media types can be selected here";
            moduleMediaTypeScopeDescription.textContent = "Preset choices are filtered to video/* values so upload validation stays aligned with video module restrictions.";
            moduleMediaTypeScopeFamilyValue.textContent = "video/*";
        } else {
            moduleMediaTypeScopeEyebrow.textContent = "File module";
            moduleMediaTypeScopeTitle.textContent = "All preset media families are available";
            moduleMediaTypeScopeDescription.textContent = "Use presets for common upload formats and add custom MIME types when a module needs tighter or vendor-specific rules.";
            moduleMediaTypeScopeFamilyValue.textContent = "all";
        }
    }
}

function syncModuleFields() {
    if (!imageFields || !videoFields) {
        return;
    }
    const value = moduleTypeSelect ? moduleTypeSelect.value : "";
    const showImage = value === "IMAGE";
    const showVideo = value === "VIDEO";

    imageFields.hidden = !showImage;
    videoFields.hidden = !showVideo;
    syncModuleMediaSettings(value);
}

if (moduleTypeSelect) {
    moduleTypeSelect.addEventListener("change", syncModuleFields);
    syncModuleFields();
}

function syncDatabasePort() {
    if (!databaseVendorSelect || !databasePortInput) {
        return;
    }
    const selectedVendor = databaseVendorSelect.value;
    if (vendorPorts[selectedVendor]) {
        databasePortInput.value = vendorPorts[selectedVendor];
    }
}

if (databaseVendorSelect) {
    databaseVendorSelect.addEventListener("change", syncDatabasePort);
}

if (showDatabaseFormButton && databaseFormPanel) {
    showDatabaseFormButton.addEventListener("click", () => {
        databaseFormPanel.classList.remove("is-hidden");
        showDatabaseFormButton.classList.add("is-hidden");
    });
}

if (cancelDatabaseFormButton && databaseFormPanel && showDatabaseFormButton) {
    cancelDatabaseFormButton.addEventListener("click", () => {
        databaseFormPanel.classList.add("is-hidden");
        showDatabaseFormButton.classList.remove("is-hidden");
    });
}

function openProjectDialog() {
    if (!projectDialog) {
        return;
    }
    if (typeof projectDialog.showModal === "function") {
        if (!projectDialog.open) {
            projectDialog.showModal();
        }
        return;
    }
    projectDialog.setAttribute("open", "open");
}

function closeProjectDialog() {
    if (!projectDialog) {
        return;
    }
    if (typeof projectDialog.close === "function" && projectDialog.open) {
        projectDialog.close();
        return;
    }
    projectDialog.removeAttribute("open");
}

projectDialogOpenButtons.forEach(button => {
    button.addEventListener("click", openProjectDialog);
});

projectDialogCloseButtons.forEach(button => {
    button.addEventListener("click", closeProjectDialog);
});

const activeStorageRoot = dashboardApp?.dataset.activeStorageRoot ?? "";

function syncStorageSettingsPreview() {
    if (!storageRootInput || !storageActivationState) {
        return;
    }
    const nextRoot = storageRootInput.value.trim() || activeStorageRoot;
    const changingRoot = nextRoot !== activeStorageRoot;
    if (storageSavedPathValue) {
        storageSavedPathValue.textContent = nextRoot;
    }
    storageActivationState.textContent = changingRoot
        ? "This saved path will become active after restart."
        : "Saved root already matches the active path.";
}

if (storageSettingsForm && storageRootInput) {
    storageRootInput.addEventListener("input", syncStorageSettingsPreview);
    syncStorageSettingsPreview();
    storageSettingsForm.addEventListener("submit", event => {
        const nextRoot = storageRootInput.value.trim();
        if (nextRoot && nextRoot !== activeStorageRoot) {
            const confirmed = window.confirm("Change the storage root after restart? Existing files are not moved automatically.");
            if (!confirmed) {
                event.preventDefault();
            }
        }
    });
}

function syncApiSecurityFields() {
    if (apiJwtValidationModes.length > 0 && apiJwtSharedSecretField && apiJwtJwkSetUriField) {
        const selectedMode = [...apiJwtValidationModes].find(option => option.checked);
        const mode = selectedMode ? selectedMode.value : "";
        apiJwtSharedSecretField.hidden = mode !== "SHARED_SECRET";
        apiJwtJwkSetUriField.hidden = mode !== "JWK_SET_URI";
        apiJwtSharedSecretField.querySelectorAll("input, select, textarea").forEach(field => {
            field.disabled = apiJwtSharedSecretField.hidden;
        });
        apiJwtJwkSetUriField.querySelectorAll("input, select, textarea").forEach(field => {
            field.disabled = apiJwtJwkSetUriField.hidden;
        });
    }
    if (apiBasicAuthEnabled && apiBasicAuthFields) {
        apiBasicAuthFields.hidden = !apiBasicAuthEnabled.checked;
        apiBasicAuthFields.querySelectorAll("input, select, textarea").forEach(field => {
            if (field !== apiBasicAuthEnabled) {
                field.disabled = apiBasicAuthFields.hidden;
            }
        });
    }
}

apiJwtValidationModes.forEach(option => option.addEventListener("change", syncApiSecurityFields));
if (apiBasicAuthEnabled) {
    apiBasicAuthEnabled.addEventListener("change", syncApiSecurityFields);
}
if (apiSecurityForm) {
    syncApiSecurityFields();
}

function setActiveTab(tabName) {
    tabButtons.forEach(button => {
        button.classList.toggle("is-active", button.dataset.tabTarget === tabName);
    });
    tabPanels.forEach(panel => {
        panel.classList.toggle("is-active", panel.dataset.tabPanel === tabName);
    });
}

tabButtons.forEach(button => {
    button.addEventListener("click", () => {
        const tabName = button.dataset.tabTarget;
        setActiveTab(tabName);
        window.history.replaceState(null, "", "#" + tabName);
    });
});

copyCurlButtons.forEach(button => {
    const defaultLabel = button.innerHTML;
    button.addEventListener("click", async () => {
        const curlCommand = button.dataset.curlTemplate.replaceAll("__BASE_URL__", window.location.origin);
        try {
            await navigator.clipboard.writeText(curlCommand);
            button.classList.add("is-copied");
            button.textContent = "Copied";
            window.setTimeout(() => {
                button.classList.remove("is-copied");
                button.innerHTML = defaultLabel;
            }, 1600);
        } catch (error) {
            button.textContent = "Copy failed";
            window.setTimeout(() => {
                button.innerHTML = defaultLabel;
            }, 1600);
        }
    });
});

const serverActiveTab = dashboardApp?.dataset.serverActiveTab ?? "";
const projectDialogShouldOpen = parseBoolean(dashboardApp?.dataset.projectDialogOpen ?? "false");
const initialTab = serverActiveTab || (window.location.hash ? window.location.hash.substring(1) : "docs");

if ([...tabPanels].some(panel => panel.dataset.tabPanel === initialTab)) {
    setActiveTab(initialTab);
}
if (projectDialogShouldOpen) {
    openProjectDialog();
}
