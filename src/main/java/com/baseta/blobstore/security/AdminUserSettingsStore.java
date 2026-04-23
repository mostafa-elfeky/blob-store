package com.baseta.blobstore.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminUserSettingsStore {

    private final AdminUserRuntimeProperties runtimeProperties;

    public AdminUserSettings loadOrCreate() {
        Path configPath = configPath();
        if (!Files.exists(configPath)) {
            return createDefault(configPath);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read admin user configuration", exception);
        }

        String username = normalizeRequired(properties.getProperty("username"), "username");
        String password = normalizeRequired(properties.getProperty("password"), "password");
        return new AdminUserSettings(username, password);
    }

    private AdminUserSettings createDefault(Path configPath) {
        String generatedPassword = UUID.randomUUID().toString();
        AdminUserSettings settings = new AdminUserSettings("admin", generatedPassword);

        try {
            Files.createDirectories(Objects.requireNonNull(configPath.getParent()));
            Properties properties = new Properties();
            properties.setProperty("username", settings.username());
            properties.setProperty("password", settings.password());
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "Blob Store admin login");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create admin user configuration", exception);
        }

        log.warn(
                "Created default admin credentials at '{}'. Username: '{}', Password: '{}'. Change this file before exposing the app.",
                configPath,
                settings.username(),
                settings.password()
        );
        return settings;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Admin user configuration is missing " + fieldName);
        }
        return value.trim();
    }

    private Path configPath() {
        return Path.of(runtimeProperties.getConfigFile()).toAbsolutePath().normalize();
    }
}
