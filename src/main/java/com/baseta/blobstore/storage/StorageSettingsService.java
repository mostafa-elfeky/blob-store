package com.baseta.blobstore.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageSettingsService {

    private final StorageRuntimeProperties runtimeProperties;
    private final StorageSettingsStore storageSettingsStore;

    private Path activeRootPath;

    @PostConstruct
    void initialize() {
        activeRootPath = resolveRootPath(
                storageSettingsStore.loadRootDir().orElse(runtimeProperties.getRootDir())
        );
        ensureDirectory(activeRootPath);
        log.info("Using storage root '{}'", activeRootPath);
    }

    public Path currentRootPath() {
        return activeRootPath;
    }

    public StorageSettingsForm currentForm() {
        StorageSettingsForm form = new StorageSettingsForm();
        form.setRootDir(storageSettingsStore.loadRootDir().orElse(activeRootPath.toString()));
        return form;
    }

    public StorageSettingsStatus currentStatus() {
        String savedRootDir = storageSettingsStore.loadRootDir().orElse(null);
        return new StorageSettingsStatus(
                activeRootPath.toString(),
                savedRootDir,
                savedRootDir != null && !resolveRootPath(savedRootDir).equals(activeRootPath)
        );
    }

    public void save(StorageSettingsForm form) {
        Path savedRootPath = resolveRootPath(form.getRootDir());
        boolean changingRoot = !savedRootPath.equals(activeRootPath);
        if (changingRoot && !form.isAcknowledgeRisk()) {
            throw new IllegalArgumentException("Confirm the storage root change before saving");
        }
        if (!changingRoot && storageSettingsStore.loadRootDir()
                .map(this::resolveRootPath)
                .filter(savedPath -> savedPath.equals(savedRootPath))
                .isPresent()) {
            log.info("Storage root setting unchanged: '{}'", savedRootPath);
            return;
        }
        storageSettingsStore.saveRootDir(savedRootPath.toString());
        log.info("Saved storage root setting '{}'; restart required to apply", savedRootPath);
    }

    private Path resolveRootPath(String rootDir) {
        return Path.of(rootDir).toAbsolutePath().normalize();
    }

    private void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create storage root directory", exception);
        }
    }
}
