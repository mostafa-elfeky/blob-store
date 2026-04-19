package com.baseta.blobstore.database;

import javax.sql.DataSource;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(DatabaseRuntimeProperties.class)
public class DatabaseConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(DatabaseConnectionService databaseConnectionService) {
        return databaseConnectionService.createDataSource();
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            DatabaseConnectionService databaseConnectionService
    ) {
        return hibernateProperties -> databaseConnectionService.currentSettings().ifPresent(settings -> {
            hibernateProperties.put("hibernate.dialect", settings.getVendor().hibernateDialect());
            hibernateProperties.put("hibernate.boot.allow_jdbc_metadata_access", false);

            if (!databaseConnectionService.currentStatus().isDatabaseReady()) {
                hibernateProperties.put("hibernate.hbm2ddl.auto", "none");
            }

            if (StringUtils.hasText(settings.getSchema()) && settings.getVendor().supportsSchemaSelection()) {
                hibernateProperties.put("hibernate.default_schema", settings.getSchema());
            }
        });
    }
}
