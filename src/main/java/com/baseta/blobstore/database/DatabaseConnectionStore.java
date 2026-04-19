package com.baseta.blobstore.database;

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
public class DatabaseConnectionStore {

    private final DatabaseRuntimeProperties runtimeProperties;

    public Optional<DatabaseConnectionSettings> load() {
        Path filePath = configPath();
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return Optional.of(new DatabaseConnectionSettings(
                    DatabaseVendor.valueOf(properties.getProperty("vendor")),
                    properties.getProperty("host"),
                    Integer.parseInt(properties.getProperty("port")),
                    properties.getProperty("databaseName"),
                    properties.getProperty("schema"),
                    properties.getProperty("username"),
                    properties.getProperty("password", "")
            ));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to read database configuration", exception);
        }
    }

    public void save(DatabaseConnectionSettings settings) {
        try {
            Path filePath = configPath();
            Files.createDirectories(filePath.getParent());
            Properties properties = new Properties();
            properties.setProperty("vendor", settings.getVendor().name());
            properties.setProperty("host", settings.getHost());
            properties.setProperty("port", Integer.toString(settings.getPort()));
            properties.setProperty("databaseName", settings.getDatabaseName());
            properties.setProperty("schema", settings.getSchema() == null ? "" : settings.getSchema());
            properties.setProperty("username", settings.getUsername());
            properties.setProperty("password", settings.getPassword());
            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                properties.store(outputStream, "Blob Store database connection");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save database configuration", exception);
        }
    }

    private Path configPath() {
        return Path.of(runtimeProperties.getConfigFile()).toAbsolutePath().normalize();
    }
}
