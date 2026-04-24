package com.baseta.blobstore.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiSecurityForm {

    @NotNull
    private ApiJwtValidationMode jwtValidationMode;

    @Size(max = 4000)
    private String jwtSharedSecret;

    @Size(max = 2000)
    private String jwtJwkSetUri;

    @Size(max = 500)
    private String jwtIssuer;

    @Size(max = 500)
    private String jwtAudience;

    private boolean basicAuthEnabled;

    @Size(max = 150)
    private String basicUsername;

    @Size(max = 500)
    private String basicPassword;
}
