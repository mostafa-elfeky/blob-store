package com.baseta.blobstore.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DatabaseStatus {

    private final String vendor;
    private final String jdbcUrl;
    private final boolean databaseReady;
    private final boolean savedConnectionConfigured;
    private final boolean usingEmbeddedDatabase;
    private final String startupMessage;

    static DatabaseStatus external(DatabaseConnectionSettings settings) {
        return new DatabaseStatus(
                settings.getVendor().displayName(),
                settings.getJdbcUrl(),
                true,
                true,
                false,
                null
        );
    }

    static DatabaseStatus embedded(String activeJdbcUrl) {
        return new DatabaseStatus(
                "Embedded H2",
                activeJdbcUrl,
                true,
                false,
                true,
                "No external database is configured. The application is running on embedded H2."
        );
    }

    static DatabaseStatus embeddedWithMessage(String activeJdbcUrl, String startupMessage) {
        return new DatabaseStatus(
                "Embedded H2",
                activeJdbcUrl,
                true,
                false,
                true,
                startupMessage
        );
    }

    static DatabaseStatus unavailable(DatabaseConnectionSettings savedSettings, String startupMessage) {
        return new DatabaseStatus(
                savedSettings.getVendor().displayName(),
                savedSettings.getJdbcUrl(),
                false,
                true,
                false,
                startupMessage
        );
    }
}
