package com.baseta.blobstore.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiSecurityStatus {

    private final ApiJwtValidationMode activeJwtValidationMode;
    private final String activeJwtIssuer;
    private final String activeJwtAudience;
    private final String activeJwtJwkSetUri;
    private final boolean activeBasicAuthEnabled;
    private final String activeBasicUsername;
    private final ApiJwtValidationMode savedJwtValidationMode;
    private final String savedJwtIssuer;
    private final String savedJwtAudience;
    private final String savedJwtJwkSetUri;
    private final boolean savedBasicAuthEnabled;
    private final String savedBasicUsername;
    private final boolean restartRequired;
}
