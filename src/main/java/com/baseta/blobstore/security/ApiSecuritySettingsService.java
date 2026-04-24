package com.baseta.blobstore.security;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiSecuritySettingsService {

    public enum SaveResult {
        UPDATED,
        UNCHANGED
    }

    private static final List<SimpleGrantedAuthority> BASIC_AUTH_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("SCOPE_blobstore.modules.read"),
            new SimpleGrantedAuthority("SCOPE_blobstore.modules.write"),
            new SimpleGrantedAuthority("SCOPE_blobstore.files.write")
    );

    private final ApiSecurityRuntimeProperties runtimeProperties;
    private final ApiSecuritySettingsStore apiSecuritySettingsStore;

    private ApiSecuritySettings activeSettings;

    @PostConstruct
    void initialize() {
        activeSettings = apiSecuritySettingsStore.load().orElse(defaultSettings());
        log.info(
                "Using API security mode '{}' with basic auth {}",
                activeSettings.jwtValidationMode(),
                activeSettings.basicAuthEnabled() ? "enabled" : "disabled"
        );
    }

    public ApiSecuritySettings currentSettings() {
        return activeSettings;
    }

    public ApiSecurityForm currentForm() {
        ApiSecuritySettings settings = apiSecuritySettingsStore.load().orElse(activeSettings);
        ApiSecurityForm form = new ApiSecurityForm();
        form.setJwtValidationMode(settings.jwtValidationMode());
        form.setJwtSharedSecret(settings.jwtSharedSecret());
        form.setJwtJwkSetUri(settings.jwtJwkSetUri());
        form.setJwtIssuer(settings.jwtIssuer());
        form.setJwtAudience(settings.jwtAudience());
        form.setBasicAuthEnabled(settings.basicAuthEnabled());
        form.setBasicUsername(settings.basicUsername());
        form.setBasicPassword(settings.basicPassword());
        return form;
    }

    public ApiSecurityStatus currentStatus() {
        ApiSecuritySettings savedSettings = apiSecuritySettingsStore.load().orElse(null);
        return new ApiSecurityStatus(
                activeSettings.jwtValidationMode(),
                activeSettings.jwtIssuer(),
                activeSettings.jwtAudience(),
                activeSettings.jwtJwkSetUri(),
                activeSettings.basicAuthEnabled(),
                activeSettings.basicUsername(),
                savedSettings == null ? null : savedSettings.jwtValidationMode(),
                savedSettings == null ? null : savedSettings.jwtIssuer(),
                savedSettings == null ? null : savedSettings.jwtAudience(),
                savedSettings == null ? null : savedSettings.jwtJwkSetUri(),
                savedSettings != null && savedSettings.basicAuthEnabled(),
                savedSettings == null ? null : savedSettings.basicUsername(),
                savedSettings != null && !savedSettings.equals(activeSettings)
        );
    }

    public SaveResult save(ApiSecurityForm form) {
        ApiSecuritySettings settings = toSettings(form);
        ApiSecuritySettings savedSettings = apiSecuritySettingsStore.load().orElse(null);
        if (settings.equals(savedSettings)) {
            log.info("API security settings unchanged");
            return SaveResult.UNCHANGED;
        }
        apiSecuritySettingsStore.save(settings);
        log.info(
                "Saved API security settings with JWT mode '{}' and basic auth {}. Restart required to apply.",
                settings.jwtValidationMode(),
                settings.basicAuthEnabled() ? "enabled" : "disabled"
        );
        return SaveResult.UPDATED;
    }

    public UserDetails loadBasicIntegrationUser(String username, PasswordEncoder passwordEncoder) {
        ApiSecuritySettings settings = currentSettings();
        if (!settings.basicAuthEnabled()) {
            throw new UsernameNotFoundException("API basic auth is disabled");
        }
        if (!settings.basicUsername().equals(username)) {
            throw new UsernameNotFoundException("Unknown API integration user");
        }
        return User.withUsername(settings.basicUsername())
                .password(passwordEncoder.encode(settings.basicPassword()))
                .authorities(BASIC_AUTH_AUTHORITIES)
                .build();
    }

    private ApiSecuritySettings toSettings(ApiSecurityForm form) {
        String jwtSharedSecret = normalize(form.getJwtSharedSecret());
        String jwtJwkSetUri = normalize(form.getJwtJwkSetUri());
        String jwtIssuer = normalize(form.getJwtIssuer());
        String jwtAudience = normalize(form.getJwtAudience());
        String basicUsername = normalize(form.getBasicUsername());
        String basicPassword = normalize(form.getBasicPassword());

        if (form.getJwtValidationMode() == ApiJwtValidationMode.SHARED_SECRET && !StringUtils.hasText(jwtSharedSecret)) {
            throw new IllegalArgumentException("Shared secret is required for shared-secret JWT mode");
        }
        if (form.getJwtValidationMode() == ApiJwtValidationMode.JWK_SET_URI && !StringUtils.hasText(jwtJwkSetUri)) {
            throw new IllegalArgumentException("JWK Set URI is required for JWK-based JWT mode");
        }
        if (form.isBasicAuthEnabled()) {
            if (!StringUtils.hasText(basicUsername)) {
                throw new IllegalArgumentException("Basic auth username is required when Basic Auth is enabled");
            }
            if (!StringUtils.hasText(basicPassword)) {
                throw new IllegalArgumentException("Basic auth password is required when Basic Auth is enabled");
            }
        } else {
            basicUsername = null;
            basicPassword = null;
        }

        return new ApiSecuritySettings(
                form.getJwtValidationMode(),
                jwtSharedSecret,
                jwtJwkSetUri,
                jwtIssuer,
                jwtAudience,
                form.isBasicAuthEnabled(),
                basicUsername,
                basicPassword
        );
    }

    private ApiSecuritySettings defaultSettings() {
        return new ApiSecuritySettings(
                ApiJwtValidationMode.SHARED_SECRET,
                runtimeProperties.getDefaultJwtSecret(),
                null,
                null,
                null,
                false,
                null,
                null
        );
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
