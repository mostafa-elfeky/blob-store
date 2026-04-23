package com.baseta.blobstore.module;

import java.text.Normalizer;
import java.util.Locale;

public final class ModuleCodeNormalizer {

    private ModuleCodeNormalizer() {
    }

    public static String normalize(String value, String fieldName) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
