package com.baseta.blobstore.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageSettingsStore {

    private final StorageRuntimeProperties runtimeProperties;

    public Optional<String> loadRootDir() {
        Path filePath = configPath();
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return Optional.ofNullable(properties.getProperty("rootDir"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read storage configuration", exception);
        }
    }

    public void saveRootDir(String rootDir) {
        try {
            Path filePath = configPath();
            Files.createDirectories(filePath.getParent());
            Properties properties = new Properties();
            properties.setProperty("rootDir", rootDir);
            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                properties.store(outputStream, "Blob Store storage settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save storage configuration", exception);
        }
    }

    private Path configPath() {
        return Path.of(runtimeProperties.getConfigFile()).toAbsolutePath().normalize();
    }
}
