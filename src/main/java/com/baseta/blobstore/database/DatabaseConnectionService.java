package com.baseta.blobstore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseConnectionService {

    private final DatabaseRuntimeProperties runtimeProperties;
    private final DatabaseConnectionStore databaseConnectionStore;
    private HikariDataSource activeDataSource;
    private DatabaseStatus databaseStatus;

    public synchronized DataSource createDataSource() {
        if (activeDataSource != null) {
            return activeDataSource;
        }

        Optional<DatabaseConnectionSettings> savedSettings;
        try {
            savedSettings = databaseConnectionStore.load();
        } catch (RuntimeException exception) {
            log.warn("Failed to read saved database configuration. Falling back to embedded H2.", exception);
            activeDataSource = buildDataSource(embeddedSettings());
            databaseStatus = DatabaseStatus.embeddedWithMessage(
                    activeDataSource.getJdbcUrl(),
                    "Saved database configuration could not be read. The application is running on embedded H2."
            );
            return activeDataSource;
        }
        if (savedSettings.isPresent()) {
            try {
                activeDataSource = buildDataSource(savedSettings.get());
                verifyConnection(activeDataSource, savedSettings.get());
                databaseStatus = DatabaseStatus.external(savedSettings.get());
                return activeDataSource;
            } catch (RuntimeException exception) {
                log.warn("Failed to initialize configured database. Starting in database-fix mode.", exception);
                closeQuietly(activeDataSource);
                activeDataSource = buildUnverifiedDataSource(savedSettings.get());
                databaseStatus = DatabaseStatus.unavailable(savedSettings.get(), rootMessage(exception));
                return activeDataSource;
            }
        }

        activeDataSource = buildDataSource(embeddedSettings());
        databaseStatus = DatabaseStatus.embedded(activeDataSource.getJdbcUrl());
        return activeDataSource;
    }

    public DatabaseConnectionForm currentForm() {
        return DatabaseConnectionForm.fromSettings(databaseConnectionStore.load().orElse(defaultExternalSettings()));
    }

    public DatabaseStatus currentStatus() {
        if (databaseStatus == null) {
            createDataSource();
        } else if (databaseStatus.isSavedConnectionConfigured() && activeDataSource != null) {
            DatabaseConnectionSettings settings = currentSettings().orElse(null);
            if (settings != null) {
                try {
                    verifyConnection(activeDataSource, settings);
                    if (!databaseStatus.isDatabaseReady()) {
                        databaseStatus = DatabaseStatus.external(settings);
                    }
                } catch (RuntimeException exception) {
                    databaseStatus = DatabaseStatus.unavailable(settings, exception.getMessage());
                }
            }
        }
        return databaseStatus;
    }

    public Optional<DatabaseConnectionSettings> currentSettings() {
        try {
            return databaseConnectionStore.load();
        } catch (RuntimeException exception) {
            log.warn("Failed to read saved database configuration", exception);
            return Optional.empty();
        }
    }

    public void test(DatabaseConnectionForm form) {
        DatabaseConnectionSettings settings = toSettings(form);
        loadDriver(settings.getVendor());
        try (Connection ignored = DriverManager.getConnection(settings.getJdbcUrl(), settings.getUsername(), settings.getPassword())) {
            log.info("Successfully tested {} database connection for host '{}'", settings.getVendor().displayName(), settings.getHost());
            // Connection established successfully.
        } catch (SQLException exception) {
            throw new IllegalArgumentException("Failed to connect to the database: " + exception.getMessage(), exception);
        }
    }

    public void save(DatabaseConnectionForm form) {
        DatabaseConnectionSettings settings = toSettings(form);
        databaseConnectionStore.save(settings);
        log.info("Saved {} database configuration for host '{}'; restart required to apply", settings.getVendor().displayName(), settings.getHost());
    }

    @PreDestroy
    void shutdown() {
        closeQuietly(activeDataSource);
    }

    private DatabaseConnectionSettings toSettings(DatabaseConnectionForm form) {
        return new DatabaseConnectionSettings(
                form.getVendor(),
                form.getHost().trim(),
                form.getPort(),
                form.getDatabaseName().trim(),
                StringUtils.hasText(form.getSchema()) ? form.getSchema().trim() : null,
                form.getUsername().trim(),
                form.getPassword()
        );
    }

    private DatabaseConnectionSettings defaultExternalSettings() {
        return new DatabaseConnectionSettings(
                DatabaseVendor.POSTGRESQL,
                "localhost",
                DatabaseVendor.POSTGRESQL.defaultPort(),
                "blobstore",
                null,
                "blobstore",
                ""
        );
    }

    private DatabaseConnectionSettings embeddedSettings() {
        return new DatabaseConnectionSettings(
                DatabaseVendor.H2,
                "localhost",
                DatabaseVendor.H2.defaultPort(),
                runtimeProperties.getBootstrapDatabasePath(),
                null,
                "sa",
                ""
        );
    }

    private HikariDataSource buildDataSource(DatabaseConnectionSettings settings) {
        return buildDataSource(settings, true);
    }

    private HikariDataSource buildUnverifiedDataSource(DatabaseConnectionSettings settings) {
        return buildDataSource(settings, false);
    }

    private HikariDataSource buildDataSource(DatabaseConnectionSettings settings, boolean initializeSchema) {
        loadDriver(settings.getVendor());
        HikariConfig config = new HikariConfig();
        config.setPoolName("blob-store");
        config.setDriverClassName(settings.getVendor().driverClassName());
        config.setJdbcUrl(settings.getJdbcUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(3000);
        config.setValidationTimeout(2000);
        config.setKeepaliveTime(0);
        if (StringUtils.hasText(settings.getSchema()) && settings.getVendor().supportsSchemaSelection()) {
            config.setSchema(settings.getSchema());
        }
        HikariDataSource dataSource = new HikariDataSource(config);
        if (initializeSchema) {
            initializeSchema(dataSource, settings);
        }
        return dataSource;
    }

    private void initializeSchema(HikariDataSource dataSource, DatabaseConnectionSettings settings) {
        if (!StringUtils.hasText(settings.getSchema())) {
            return;
        }

        String schema = settings.getSchema().trim();
        switch (settings.getVendor()) {
            case POSTGRESQL -> executeSchemaStatement(
                    dataSource,
                    "create schema if not exists " + quoteIdentifier(schema)
            );
            case SQLSERVER -> executeSchemaStatement(
                    dataSource,
                    "if not exists (select * from sys.schemas where name = '" + escapeSqlLiteral(schema)
                            + "') exec('create schema " + quoteSqlServerIdentifier(schema) + "')"
            );
            default -> {
                // MySQL uses the selected database as the schema and Oracle schemas map to users.
            }
        }
    }

    private void executeSchemaStatement(HikariDataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize the configured schema", exception);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String quoteSqlServerIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private void verifyConnection(HikariDataSource dataSource, DatabaseConnectionSettings settings) {
        try (Connection ignored = dataSource.getConnection()) {
            // Verified successfully.
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to connect using the saved database configuration", exception);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private void loadDriver(DatabaseVendor vendor) {
        try {
            Class.forName(vendor.driverClassName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Database driver is not available for " + vendor.displayName(), exception);
        }
    }

    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
