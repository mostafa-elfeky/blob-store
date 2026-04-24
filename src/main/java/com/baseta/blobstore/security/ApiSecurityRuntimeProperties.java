package com.baseta.blobstore.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blobstore.api-security")
public class ApiSecurityRuntimeProperties {

    private String configFile;
    private String defaultJwtSecret;

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getDefaultJwtSecret() {
        return defaultJwtSecret;
    }

    public void setDefaultJwtSecret(String defaultJwtSecret) {
        this.defaultJwtSecret = defaultJwtSecret;
    }
}
