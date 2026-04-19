package com.baseta.blobstore.database;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DatabaseConnectionForm {

    @NotNull
    private DatabaseVendor vendor;

    @NotBlank
    @Size(max = 255)
    private String host;

    @NotNull
    @Min(1)
    private Integer port;

    @NotBlank
    @Size(max = 255)
    private String databaseName;

    @Size(max = 255)
    private String schema;

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String password;

    public static DatabaseConnectionForm fromSettings(DatabaseConnectionSettings settings) {
        DatabaseConnectionForm form = new DatabaseConnectionForm();
        form.setVendor(settings.getVendor());
        form.setHost(settings.getHost());
        form.setPort(settings.getPort());
        form.setDatabaseName(settings.getDatabaseName());
        form.setSchema(settings.getSchema());
        form.setUsername(settings.getUsername());
        form.setPassword(settings.getPassword());
        return form;
    }
}
