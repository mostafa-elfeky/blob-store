package com.baseta.blobstore.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class ImageSizeDefinitionParser {

    List<ImageSizeDefinition> parse(String source) {
        String normalizedSource = source == null ? "" : source.trim();
        if (normalizedSource.isBlank()) {
            throw new IllegalArgumentException("At least one image size is required for image modules");
        }

        List<ImageSizeDefinition> sizes = new ArrayList<>();
        for (String line : normalizedSource.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            sizes.add(parseLine(trimmed, sizes));
        }

        if (sizes.isEmpty()) {
            throw new IllegalArgumentException("At least one image size is required for image modules");
        }
        return sizes;
    }

    private ImageSizeDefinition parseLine(String line, List<ImageSizeDefinition> sizes) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid image size format. Use code=WIDTHxHEIGHT");
        }

        String code = ModuleCodeNormalizer.normalize(parts[0], "Image size code");
        if (isDuplicateCode(code, sizes)) {
            throw new IllegalArgumentException("Duplicate image size code: " + code);
        }

        ImageDimensions dimensions = parseDimensions(parts[1], "Invalid image size dimensions. Use code=WIDTHxHEIGHT");
        return new ImageSizeDefinition(code, dimensions.width(), dimensions.height());
    }

    ImageDimensions parseOptionalOriginalSize(String source) {
        String normalizedSource = source == null ? "" : source.trim();
        if (normalizedSource.isBlank()) {
            return null;
        }
        return parseDimensions(normalizedSource, "Invalid original image size. Use WIDTHxHEIGHT");
    }

    private ImageDimensions parseDimensions(String source, String invalidFormatMessage) {
        String[] dimensions = source.trim().toLowerCase(Locale.ROOT).split("x", 2);
        if (dimensions.length != 2) {
            throw new IllegalArgumentException(invalidFormatMessage);
        }
        return new ImageDimensions(parseDimension(dimensions[0]), parseDimension(dimensions[1]));
    }

    private boolean isDuplicateCode(String code, List<ImageSizeDefinition> sizes) {
        return sizes.stream()
                .map(ImageSizeDefinition::getCode)
                .anyMatch(code::equals);
    }

    private Integer parseDimension(String value) {
        try {
            int dimension = Integer.parseInt(value.trim());
            if (dimension < 1) {
                throw new IllegalArgumentException("Image size width and height must be positive");
            }
            return dimension;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid image size dimensions. Use numeric WIDTHxHEIGHT");
        }
    }
}
