package com.baseta.blobstore.module;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageSizeDefinitionParserTest {

    private final ImageSizeDefinitionParser parser = new ImageSizeDefinitionParser();

    @Test
    void shouldParseAndNormalizeImageSizes() {
        List<ImageSizeDefinition> definitions = parser.parse(" Thumb = 80x80\nhero-banner = 1920x1080 ");

        assertThat(definitions)
                .extracting(ImageSizeDefinition::getCode, ImageSizeDefinition::getWidth, ImageSizeDefinition::getHeight)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("thumb", 80, 80),
                        org.assertj.core.groups.Tuple.tuple("hero-banner", 1920, 1080)
                );
    }

    @Test
    void shouldRejectDuplicateCodes() {
        assertThatThrownBy(() -> parser.parse("thumb=80x80\nthumb=120x120"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate image size code: thumb");
    }

    @Test
    void shouldRejectInvalidDimensions() {
        assertThatThrownBy(() -> parser.parse("thumb=wide x 80"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid image size dimensions. Use numeric WIDTHxHEIGHT");
    }

    @Test
    void shouldParseOptionalOriginalImageSize() {
        ImageDimensions dimensions = parser.parseOptionalOriginalSize(" 1200x630 ");

        assertThat(dimensions).isEqualTo(new ImageDimensions(1200, 630));
    }

    @Test
    void shouldAllowBlankOptionalOriginalImageSize() {
        assertThat(parser.parseOptionalOriginalSize("   ")).isNull();
    }

    @Test
    void shouldRejectInvalidOptionalOriginalImageSize() {
        assertThatThrownBy(() -> parser.parseOptionalOriginalSize("1200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid original image size. Use WIDTHxHEIGHT");
    }
}
