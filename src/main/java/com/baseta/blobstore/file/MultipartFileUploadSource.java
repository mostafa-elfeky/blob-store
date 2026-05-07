package com.baseta.blobstore.file;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
class MultipartFileUploadSource implements FileUploadSource {

    private final MultipartFile multipartFile;

    @Override
    public String getOriginalName() {
        return multipartFile.getOriginalFilename();
    }

    @Override
    public String getContentType() {
        return multipartFile.getContentType();
    }

    @Override
    public long getSize() {
        return multipartFile.getSize();
    }

    @Override
    public boolean isEmpty() {
        return multipartFile.isEmpty();
    }

    @Override
    public InputStream openStream() throws IOException {
        return multipartFile.getInputStream();
    }
}
