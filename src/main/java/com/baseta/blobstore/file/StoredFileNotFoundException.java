package com.baseta.blobstore.file;

import java.util.UUID;

public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(UUID fileKey) {
        super("File not found: " + fileKey);
    }

    public StoredFileNotFoundException(String retrievalName) {
        super("File not found: " + retrievalName);
    }
}
