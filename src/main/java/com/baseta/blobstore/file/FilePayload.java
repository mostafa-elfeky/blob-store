package com.baseta.blobstore.file;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FilePayload {

    private final String fileKey;
    private final String moduleCode;
    private final String originalName;
    private final String contentType;
    private final long contentLength;
    private final InputStream inputStream;
}
