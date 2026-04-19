package com.baseta.blobstore.module;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ModuleMediaTypeSupport {

    private static final Pattern MEDIA_TYPE_PATTERN = Pattern.compile("^[a-z0-9!#$&^_.+-]+/(\\*|[a-z0-9!#$&^_.+-]+)$");
    private static final List<String> PRESET_MEDIA_TYPES = List.of(
            "image/avif",
            "image/jpeg",
            "image/png",
            "image/svg+xml",
            "image/webp",
            "image/gif",
            "video/avi",
            "video/mpeg",
            "video/mp4",
            "video/ogg",
            "video/webm",
            "video/quicktime",
            "audio/aac",
            "audio/mpeg",
            "audio/ogg",
            "audio/wav",
            "application/zip",
            "application/x-zip-compressed",
            "application/pdf",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/json",
            "application/xml",
            "text/csv",
            "text/plain"
    );

    private ModuleMediaTypeSupport() {
    }

    public static List<String> presetMediaTypes() {
        return PRESET_MEDIA_TYPES;
    }

    public static boolean isPreset(String mediaType) {
        return PRESET_MEDIA_TYPES.contains(mediaType);
    }

    public static List<String> resolveAllowedMediaTypes(List<String> selectedMediaTypes, String customMediaTypes) {
        Set<String> resolvedMediaTypes = new LinkedHashSet<>();
        if (selectedMediaTypes != null) {
            selectedMediaTypes.stream()
                    .filter(mediaType -> mediaType != null && !mediaType.isBlank())
                    .map(ModuleMediaTypeSupport::normalizeConfiguredMediaType)
                    .forEach(resolvedMediaTypes::add);
        }

        if (customMediaTypes != null) {
            for (String token : customMediaTypes.split("[,\\n\\r]+")) {
                if (!token.isBlank()) {
                    resolvedMediaTypes.add(normalizeConfiguredMediaType(token));
                }
            }
        }

        return List.copyOf(resolvedMediaTypes);
    }

    public static void validateSupportedMediaTypes(ModuleType moduleType, List<String> supportedMediaTypes) {
        for (String supportedMediaType : supportedMediaTypes) {
            if (!MEDIA_TYPE_PATTERN.matcher(supportedMediaType).matches()) {
                throw new IllegalArgumentException("Invalid media type: " + supportedMediaType);
            }
            if (moduleType == ModuleType.IMAGE && !supportedMediaType.startsWith("image/")) {
                throw new IllegalArgumentException("Image modules support image/* media types only");
            }
            if (moduleType == ModuleType.VIDEO && !supportedMediaType.startsWith("video/")) {
                throw new IllegalArgumentException("Video modules support video/* media types only");
            }
        }
    }

    public static boolean matches(String contentType, List<String> supportedMediaTypes) {
        String normalizedContentType = normalizeUploadedContentType(contentType);
        return supportedMediaTypes.stream().anyMatch(mediaType -> matchesSingle(normalizedContentType, mediaType));
    }

    public static String joinCustomMediaTypes(List<String> supportedMediaTypes) {
        List<String> customMediaTypes = new ArrayList<>();
        for (String supportedMediaType : supportedMediaTypes) {
            if (!isPreset(supportedMediaType)) {
                customMediaTypes.add(supportedMediaType);
            }
        }
        return customMediaTypes.isEmpty() ? null : String.join("\n", customMediaTypes);
    }

    private static boolean matchesSingle(String contentType, String configuredMediaType) {
        int slashIndex = configuredMediaType.indexOf('/');
        String configuredType = configuredMediaType.substring(0, slashIndex);
        String configuredSubtype = configuredMediaType.substring(slashIndex + 1);

        int contentSlashIndex = contentType.indexOf('/');
        if (contentSlashIndex < 0) {
            return false;
        }
        String contentTypePart = contentType.substring(0, contentSlashIndex);
        String contentSubtypePart = contentType.substring(contentSlashIndex + 1);
        if (!configuredType.equals(contentTypePart)) {
            return false;
        }
        return configuredSubtype.equals("*") || configuredSubtype.equals(contentSubtypePart);
    }

    private static String normalizeConfiguredMediaType(String mediaType) {
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUploadedContentType(String contentType) {
        String trimmed = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        int parameterIndex = trimmed.indexOf(';');
        return parameterIndex >= 0 ? trimmed.substring(0, parameterIndex).trim() : trimmed;
    }
}
