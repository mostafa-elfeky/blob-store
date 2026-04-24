package com.baseta.blobstore.security;

public record ApiSecuritySettings(
        ApiJwtValidationMode jwtValidationMode,
        String jwtSharedSecret,
        String jwtJwkSetUri,
        String jwtIssuer,
        String jwtAudience,
        boolean basicAuthEnabled,
        String basicUsername,
        String basicPassword
) {
}
