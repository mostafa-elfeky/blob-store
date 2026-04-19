package com.baseta.blobstore.database;

import java.nio.file.Path;

public enum DatabaseVendor {
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver", "org.hibernate.dialect.PostgreSQLDialect", true) {
        @Override
        public String buildJdbcUrl(String host, int port, String databaseName) {
            return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
        }
    },
    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver", "org.hibernate.dialect.MySQLDialect", false) {
        @Override
        public String buildJdbcUrl(String host, int port, String databaseName) {
            return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                    + "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        }
    },
    SQLSERVER("SQL Server", 1433, "com.microsoft.sqlserver.jdbc.SQLServerDriver", "org.hibernate.dialect.SQLServerDialect", true) {
        @Override
        public String buildJdbcUrl(String host, int port, String databaseName) {
            return "jdbc:sqlserver://" + host + ":" + port
                    + ";databaseName=" + databaseName
                    + ";encrypt=true;trustServerCertificate=true";
        }
    },
    ORACLE("Oracle", 1521, "oracle.jdbc.OracleDriver", "org.hibernate.dialect.OracleDialect", true) {
        @Override
        public String buildJdbcUrl(String host, int port, String databaseName) {
            return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + databaseName;
        }
    },
    H2("Embedded H2", 9092, "org.h2.Driver", "org.hibernate.dialect.H2Dialect", false) {
        @Override
        public String buildJdbcUrl(String host, int port, String databaseName) {
            return "jdbc:h2:file:" + Path.of(databaseName).toAbsolutePath().normalize()
                    + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        }
    };

    private final String displayName;
    private final int defaultPort;
    private final String driverClassName;
    private final String hibernateDialect;
    private final boolean supportsSchemaSelection;

    DatabaseVendor(
            String displayName,
            int defaultPort,
            String driverClassName,
            String hibernateDialect,
            boolean supportsSchemaSelection
    ) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.driverClassName = driverClassName;
        this.hibernateDialect = hibernateDialect;
        this.supportsSchemaSelection = supportsSchemaSelection;
    }

    public abstract String buildJdbcUrl(String host, int port, String databaseName);

    public String displayName() {
        return displayName;
    }

    public int defaultPort() {
        return defaultPort;
    }

    public String driverClassName() {
        return driverClassName;
    }

    public String hibernateDialect() {
        return hibernateDialect;
    }

    public boolean supportsSchemaSelection() {
        return supportsSchemaSelection;
    }
}
