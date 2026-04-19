package com.baseta.blobstore.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageRuntimeProperties.class)
public class StorageConfiguration {
}
