package com.baseta.blobstore.file;

import com.baseta.blobstore.module.ModuleEntity;
import com.baseta.blobstore.module.ModuleService;
import com.baseta.blobstore.module.ModuleType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private ModuleService moduleService;

    @Mock
    private StoredFileRepository storedFileRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    @Test
    void shouldRejectImageUploadWhenOriginalSizeDoesNotMatch() throws IOException {
        ModuleEntity module = new ModuleEntity();
        module.setCode("banners");
        module.setType(ModuleType.IMAGE);
        module.setStorageFolder("banners");
        module.setOriginalImageWidth(1200);
        module.setOriginalImageHeight(630);

        when(moduleService.getByCode("banners")).thenReturn(module);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.png",
                "image/png",
                createImageBytes(1000, 500)
        );

        assertThatThrownBy(() -> fileStorageService.store("banners", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded image must match the original size 1200x630");
    }

    private byte[] createImageBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
