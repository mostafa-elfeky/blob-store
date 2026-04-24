package com.baseta.blobstore.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ApiJwtDecoder implements JwtDecoder {

    private final ApiSecuritySettingsService apiSecuritySettingsService;

    @Override
    public Jwt decode(String token) {
        ApiSecuritySettings settings = apiSecuritySettingsService.currentSettings();
        return buildDecoder(settings).decode(token);
    }

    private JwtDecoder buildDecoder(ApiSecuritySettings settings) {
        NimbusJwtDecoder decoder = switch (settings.jwtValidationMode()) {
            case SHARED_SECRET -> buildSharedSecretDecoder(settings.jwtSharedSecret());
            case JWK_SET_URI -> NimbusJwtDecoder.withJwkSetUri(settings.jwtJwkSetUri()).build();
        };
        decoder.setJwtValidator(buildValidator(settings));
        return decoder;
    }

    private NimbusJwtDecoder buildSharedSecretDecoder(String secret) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private OAuth2TokenValidator<Jwt> buildValidator(ApiSecuritySettings settings) {
        OAuth2TokenValidator<Jwt> baseValidator = StringUtils.hasText(settings.jwtIssuer())
                ? JwtValidators.createDefaultWithIssuer(settings.jwtIssuer())
                : JwtValidators.createDefault();
        if (!StringUtils.hasText(settings.jwtAudience())) {
            return baseValidator;
        }
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(settings.jwtAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The required audience is missing", null)
        );
        return new DelegatingOAuth2TokenValidator<>(baseValidator, audienceValidator);
    }
}
