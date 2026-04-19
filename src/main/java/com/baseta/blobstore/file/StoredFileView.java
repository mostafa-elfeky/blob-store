package com.baseta.blobstore.file;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoredFileView {

    private final UUID fileKey;
    private final String moduleCode;
    private final String originalName;
    private final String retrievalName;
    private final String contentType;
    private final long fileSize;
    private final Instant createdAt;

    public static StoredFileView fromEntity(StoredFileEntity entity) {
        return new StoredFileView(
                entity.getFileKey(),
                entity.getModule().getCode(),
                entity.getOriginalName(),
                entity.getRetrievalName(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getCreatedAt()
        );
    }
}
