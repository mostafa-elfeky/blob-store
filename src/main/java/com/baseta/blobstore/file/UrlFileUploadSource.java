package com.baseta.blobstore.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

@Getter
class UrlFileUploadSource implements FileUploadSource {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String originalName;
    private final String contentType;
    private final long size;
    private final Path localPath;

    private UrlFileUploadSource(String originalName, String contentType, long size, Path localPath) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.localPath = localPath;
    }

    static UrlFileUploadSource download(String fileUrl) {
        String normalizedUrl = normalizeUrl(fileUrl);
        URI uri;
        try {
            uri = URI.create(normalizedUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("File URL is invalid", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("File URL must use http or https");
        }

        Path temporaryFile;
        try {
            temporaryFile = Files.createTempFile("blob-store-url-upload-", ".tmp");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare URL upload storage", exception);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(temporaryFile));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                deleteQuietly(temporaryFile);
                throw new IllegalArgumentException("Failed to download file from URL: received HTTP " + response.statusCode());
            }

            URI responseUri = response.uri() != null ? response.uri() : uri;
            String originalName = resolveOriginalName(responseUri);
            String contentType = resolveContentType(response, originalName);
            long size = Files.size(temporaryFile);
            return new UrlFileUploadSource(originalName, contentType, size, temporaryFile);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            deleteQuietly(temporaryFile);
            throw new IllegalStateException("File download was interrupted", exception);
        } catch (IOException exception) {
            deleteQuietly(temporaryFile);
            throw new IllegalStateException("Failed to download file from URL", exception);
        }
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(localPath);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(localPath);
    }

    private static String normalizeUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            throw new IllegalArgumentException("fileUrl is required");
        }
        return fileUrl.trim().replace(" ", "+");
    }

    private static String resolveOriginalName(URI uri) {
        String path = uri.getPath();
        if (!StringUtils.hasText(path)) {
            return "file";
        }
        int lastSlash = path.lastIndexOf('/');
        String candidate = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return StringUtils.hasText(candidate) ? candidate : "file";
    }

    private static String resolveContentType(HttpResponse<Path> response, String originalName) {
        String headerValue = response.headers()
                .firstValue("Content-Type")
                .map(UrlFileUploadSource::sanitizeContentType)
                .orElse(null);
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        String guessed = URLConnection.guessContentTypeFromName(originalName);
        return StringUtils.hasText(guessed) ? guessed : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static String sanitizeContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType).toString();
        } catch (IllegalArgumentException exception) {
            return contentType;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
