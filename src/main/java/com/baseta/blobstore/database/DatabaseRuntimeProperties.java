package com.baseta.blobstore.database;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "blobstore.database")
public class DatabaseRuntimeProperties {

    private String configFile;
    private String bootstrapDatabasePath;
}
