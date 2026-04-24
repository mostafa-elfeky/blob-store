package com.baseta.blobstore.security;

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
public class ApiSecuritySettingsStore {

    private final ApiSecurityRuntimeProperties runtimeProperties;

    public Optional<ApiSecuritySettings> load() {
        Path filePath = configPath();
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return Optional.of(new ApiSecuritySettings(
                    ApiJwtValidationMode.valueOf(properties.getProperty("jwtValidationMode")),
                    trimToNull(properties.getProperty("jwtSharedSecret")),
                    trimToNull(properties.getProperty("jwtJwkSetUri")),
                    trimToNull(properties.getProperty("jwtIssuer")),
                    trimToNull(properties.getProperty("jwtAudience")),
                    Boolean.parseBoolean(properties.getProperty("basicAuthEnabled", "false")),
                    trimToNull(properties.getProperty("basicUsername")),
                    trimToNull(properties.getProperty("basicPassword"))
            ));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to read API security configuration", exception);
        }
    }

    public void save(ApiSecuritySettings settings) {
        try {
            Path filePath = configPath();
            Files.createDirectories(filePath.getParent());
            Properties properties = new Properties();
            properties.setProperty("jwtValidationMode", settings.jwtValidationMode().name());
            properties.setProperty("jwtSharedSecret", settings.jwtSharedSecret() == null ? "" : settings.jwtSharedSecret());
            properties.setProperty("jwtJwkSetUri", settings.jwtJwkSetUri() == null ? "" : settings.jwtJwkSetUri());
            properties.setProperty("jwtIssuer", settings.jwtIssuer() == null ? "" : settings.jwtIssuer());
            properties.setProperty("jwtAudience", settings.jwtAudience() == null ? "" : settings.jwtAudience());
            properties.setProperty("basicAuthEnabled", Boolean.toString(settings.basicAuthEnabled()));
            properties.setProperty("basicUsername", settings.basicUsername() == null ? "" : settings.basicUsername());
            properties.setProperty("basicPassword", settings.basicPassword() == null ? "" : settings.basicPassword());
            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                properties.store(outputStream, "Blob Store API security settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save API security configuration", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Path configPath() {
        return Path.of(runtimeProperties.getConfigFile()).toAbsolutePath().normalize();
    }
}
