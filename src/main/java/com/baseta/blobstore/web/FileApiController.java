package com.baseta.blobstore.web;

import com.baseta.blobstore.file.FilePayload;
import com.baseta.blobstore.file.FileStorageService;
import com.baseta.blobstore.file.StoredFileView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Slf4j
@RequiredArgsConstructor
public class FileApiController {

    private static final String EXPOSE_HEADERS = String.join(
            ", ",
            HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_LENGTH
    );

    private final FileStorageService fileStorageService;

    @PostMapping(path = "/{moduleCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileView upload(
            @PathVariable String moduleCode,
            @RequestParam("file") MultipartFile file
    ) {
        return fileStorageService.store(moduleCode, file);
    }

    @GetMapping("/{fileKey}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID fileKey,
            @RequestParam(required = false, name = "type") String type
    ) throws IOException {
        FilePayload payload = fileStorageService.open(fileKey, type);
        return buildResponse(payload);
    }

    @GetMapping("/{fileKey}/metadata")
    public StoredFileView metadata(@PathVariable UUID fileKey) {
        return fileStorageService.getMetadata(fileKey);
    }

    private ResponseEntity<InputStreamResource> buildResponse(FilePayload payload) {
        String contentDisposition = ContentDisposition.inline()
                .filename(payload.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(resolveResponseMediaType(payload.getContentType()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSE_HEADERS)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentLength(payload.getContentLength())
                .body(new InputStreamResource(payload.getInputStream()));
    }

    private MediaType resolveResponseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid stored content type '{}'; falling back to application/octet-stream", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
