package com.baseta.blobstore.database;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseVendorTest {

    @Test
    void shouldBuildVendorSpecificJdbcUrls() {
        assertThat(DatabaseVendor.POSTGRESQL.buildJdbcUrl("db.local", 5432, "blobstore"))
                .isEqualTo("jdbc:postgresql://db.local:5432/blobstore");
        assertThat(DatabaseVendor.MYSQL.buildJdbcUrl("db.local", 3306, "blobstore"))
                .isEqualTo("jdbc:mysql://db.local:3306/blobstore?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
        assertThat(DatabaseVendor.SQLSERVER.buildJdbcUrl("db.local", 1433, "blobstore"))
                .isEqualTo("jdbc:sqlserver://db.local:1433;databaseName=blobstore;encrypt=true;trustServerCertificate=true");
        assertThat(DatabaseVendor.ORACLE.buildJdbcUrl("db.local", 1521, "xe"))
                .isEqualTo("jdbc:oracle:thin:@//db.local:1521/xe");
    }
}
