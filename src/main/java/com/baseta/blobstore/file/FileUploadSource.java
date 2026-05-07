package com.baseta.blobstore.file;

import java.io.IOException;
import java.io.InputStream;

interface FileUploadSource extends AutoCloseable {

    String getOriginalName();

    String getContentType();

    long getSize();

    boolean isEmpty();

    InputStream openStream() throws IOException;

    @Override
    default void close() throws IOException {
    }
}
