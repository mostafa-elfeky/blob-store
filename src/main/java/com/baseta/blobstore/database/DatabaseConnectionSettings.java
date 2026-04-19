package com.baseta.blobstore.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DatabaseConnectionSettings {

    private final DatabaseVendor vendor;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String schema;
    private final String username;
    private final String password;

    public String getJdbcUrl() {
        return vendor.buildJdbcUrl(host, port, databaseName);
    }
}
