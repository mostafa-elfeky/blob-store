package com.baseta.blobstore.storage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "blobstore.storage")
public class StorageRuntimeProperties {

    private String rootDir;
    private String configFile;
}
