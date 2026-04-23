package com.baseta.blobstore.web;

import com.baseta.blobstore.file.FilePayload;
import com.baseta.blobstore.file.FileStorageService;
import com.baseta.blobstore.file.StoredFileView;
import com.baseta.blobstore.module.ModuleEntity;
import com.baseta.blobstore.module.ModuleForm;
import com.baseta.blobstore.module.ModuleManagementService;
import com.baseta.blobstore.module.ModuleService;
import com.baseta.blobstore.module.ModuleType;
import com.baseta.blobstore.module.ModuleView;
import com.baseta.blobstore.module.VideoType;
import com.baseta.blobstore.project.ProjectEntity;
import com.baseta.blobstore.project.ProjectForm;
import com.baseta.blobstore.project.ProjectService;
import com.baseta.blobstore.storage.StorageSettingsService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "blobstore.database.config-file=build/test-config/database-connection.properties",
        "blobstore.database.bootstrap-database-path=build/test-config/bootstrap-db/blob-store",
        "blobstore.storage.config-file=build/test-config/storage-settings.properties",
        "blobstore.storage.root-dir=build/test-storage"
})
class BlobStoreApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(BlobStoreApplicationTests.class);

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ModuleManagementService moduleManagementService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StorageSettingsService storageSettingsService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetStorage() throws IOException {
        deleteDirectory(Path.of("build/test-storage"));
        deleteDirectory(Path.of("build/test-config"));
    }

    @Test
    void shouldCreateModuleAndUploadFile() throws Exception {
        Files.createDirectories(Path.of("build/test-storage"));

        ModuleForm form = new ModuleForm();
        form.setCode("product-images");
        form.setDisplayName("Product Images");
        form.setProjectId(createProject("commerce-assets", "Commerce Assets").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80\nmedium=300x300");
        form.setMaxFileSizeMb(5);

        ModuleEntity module = moduleService.create(form);
        Path moduleRoot = moduleRoot(module);

        assertThat(module.getCode()).isEqualTo("product-images");
        assertThat(Files.isDirectory(moduleRoot)).isTrue();
        assertThat(module.getImageSizes()).hasSize(2);
        assertThat(Files.isDirectory(moduleRoot.resolve("original"))).isFalse();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                createImageBytes()
        );

        StoredFileView storedFile = fileStorageService.store("product-images", file);
        FilePayload payload = fileStorageService.open(storedFile.getFileKey());
        FilePayload variantPayload = fileStorageService.open(storedFile.getFileKey(), "thumb");

        assertThat(storedFile.getModuleCode()).isEqualTo("product-images");
        assertThat(storedFile.getOriginalName()).isEqualTo("sample.jpg");
        assertThat(storedFile.getRetrievalName()).endsWith(".jpg");
        assertThat(payload.getOriginalName()).isEqualTo("sample.jpg");
        assertThat(payload.getContentLength()).isPositive();
        assertThat(variantPayload.getOriginalName()).isEqualTo("sample.jpg");
        assertThat(variantPayload.getContentLength()).isPositive();
        assertThat(Files.exists(moduleRoot.resolve("original").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("thumb").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("medium").resolve(storedFile.getRetrievalName()))).isTrue();
    }

    @Test
    void shouldRequireVideoTypeForVideoModules() {
        ModuleForm form = new ModuleForm();
        form.setCode("campaign-videos");
        form.setDisplayName("Campaign Videos");
        form.setProjectId(createProject("video-assets", "Video Assets").getId());
        form.setType(ModuleType.VIDEO);
        form.setMaxFileSizeMb(25);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> moduleService.create(form)
        );

        form.setVideoType(VideoType.SHORT);
        ModuleEntity module = moduleService.create(form);

        assertThat(module.getVideoType()).isEqualTo(VideoType.SHORT);
    }

    @Test
    void shouldRestrictFileModuleToConfiguredMediaTypes() {
        ModuleForm form = new ModuleForm();
        form.setCode("documents-only");
        form.setDisplayName("Documents Only");
        form.setProjectId(createProject("documents-team", "Documents Team").getId());
        form.setType(ModuleType.FILE);
        form.setSelectedMediaTypes(List.of("application/pdf"));
        form.setMaxFileSizeMb(25);

        ModuleEntity module = moduleService.create(form);

        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "contract.pdf", "application/pdf", "pdf-content".getBytes())
        );

        assertThat(storedFile.getOriginalName()).isEqualTo("contract.pdf");
        assertThatThrownBy(() -> fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "notes.txt", "text/plain", "plain-text".getBytes())
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file media type is not allowed");
    }

    @Test
    void shouldRestrictImageModuleToConfiguredImageMediaTypes() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("png-images");
        form.setDisplayName("PNG Images");
        form.setProjectId(createProject("media-library", "Media Library").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80");
        form.setSelectedMediaTypes(List.of("image/png"));
        form.setMaxFileSizeMb(5);

        ModuleEntity module = moduleService.create(form);

        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "poster.png", "image/png", createImageBytes())
        );

        assertThat(storedFile.getOriginalName()).isEqualTo("poster.png");
        assertThatThrownBy(() -> fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "poster.jpg", "image/jpeg", createImageBytes())
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file media type is not allowed");
    }

    @Test
    void shouldRejectNonImageMediaTypesForImageModuleRestrictions() {
        ModuleForm form = new ModuleForm();
        form.setCode("image-config");
        form.setDisplayName("Image Config");
        form.setProjectId(createProject("design-system", "Design System").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80");
        form.setCustomMediaTypes("application/pdf");

        assertThatThrownBy(() -> moduleService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image modules support image/* media types only");
    }

    @Test
    void shouldReturnContentDispositionWhenDownloading() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("documents");
        form.setDisplayName("Documents");
        form.setProjectId(createProject("legal-assets", "Legal Assets").getId());
        form.setType(ModuleType.FILE);
        form.setMaxFileSizeMb(5);

        ModuleEntity module = moduleService.create(form);
        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "contract final.pdf", "application/pdf", "pdf-content".getBytes())
        );

        mockMvc.perform(get("/api/files/{fileKey}", storedFile.getFileKey()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"=?UTF-8?Q?contract_final.pdf?=\"; filename*=UTF-8''contract%20final.pdf"));
    }

    @Test
    void shouldReturnUtf8SafeContentDispositionWhenDownloading() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("localized-documents");
        form.setDisplayName("Localized Documents");
        form.setProjectId(createProject("localization-team", "Localization Team").getId());
        form.setType(ModuleType.FILE);
        form.setMaxFileSizeMb(5);

        ModuleEntity module = moduleService.create(form);
        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "عقد نهائي.png", "image/png", createImageBytes())
        );

        mockMvc.perform(get("/api/files/{fileKey}", storedFile.getFileKey()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"=?UTF-8?Q?=D8=B9=D9=82=D8=AF_=D9=86=D9=87=D8=A7=D8=A6=D9=8A.png?=\"; filename*=UTF-8''%D8%B9%D9%82%D8%AF%20%D9%86%D9%87%D8%A7%D8%A6%D9%8A.png"));
    }

    @Test
    void shouldReturnOriginalNameInMetadataEndpoint() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("letters");
        form.setDisplayName("Letters");
        form.setProjectId(createProject("mail-room", "Mail Room").getId());
        form.setType(ModuleType.FILE);
        form.setMaxFileSizeMb(5);

        ModuleEntity module = moduleService.create(form);
        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "offer.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx-content".getBytes())
        );

        mockMvc.perform(get("/api/files/{fileKey}/metadata", storedFile.getFileKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileKey").value(storedFile.getFileKey().toString()))
                .andExpect(jsonPath("$.moduleCode").value("letters"))
                .andExpect(jsonPath("$.originalName").value("offer.docx"))
                .andExpect(jsonPath("$.retrievalName").value(storedFile.getRetrievalName()));
    }

    @Test
    void shouldUpdateModuleMediaTypesFromModuleForm() {
        ModuleForm form = new ModuleForm();
        form.setCode("invoice-files");
        form.setDisplayName("Invoice Files");
        form.setProjectId(createProject("finance-ops", "Finance Ops").getId());
        form.setType(ModuleType.FILE);

        ModuleEntity module = moduleManagementService.save(form);

        ModuleForm updateForm = moduleService.buildForm(module.getId());
        updateForm.setDisplayName("Invoice Files");
        updateForm.setSelectedMediaTypes(List.of("application/pdf"));
        updateForm.setCustomMediaTypes("application/vnd.ms-excel");

        moduleManagementService.save(updateForm);

        ModuleEntity updated = moduleService.getById(module.getId());
        assertThat(updated.getSupportedMediaTypes())
                .containsExactly("application/pdf", "application/vnd.ms-excel");
    }

    @Test
    void shouldSaveStorageRootFromSettingsPage() throws Exception {
        mockMvc.perform(post("/admin/storage-settings")
                        .param("rootDir", "build/alternate-storage")
                        .param("acknowledgeRisk", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(storageSettingsService.currentStatus().isRestartRequired()).isTrue();
        assertThat(storageSettingsService.currentForm().getRootDir())
                .isEqualTo(Path.of("build/alternate-storage").toAbsolutePath().normalize().toString());
    }

    @Test
    void shouldRenderSystemLogsInAdminDashboard() throws Exception {
        String marker = "dashboard-log-marker-" + System.nanoTime();
        logger.info(marker);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("System Logs")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(marker)));
    }

    @Test
    void shouldPermanentlyDeleteFilesBelongingToDeletedModules() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("cleanup-images");
        form.setDisplayName("Cleanup Images");
        form.setProjectId(createProject("cleanup-ops", "Cleanup Ops").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80");

        ModuleEntity module = moduleManagementService.save(form);
        Path moduleRoot = moduleRoot(module);
        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "cleanup.jpg", "image/jpeg", createImageBytes())
        );

        long deletedFileCountBeforeDelete = fileStorageService.countFilesMarkedDeleted();
        moduleManagementService.delete(module.getId());

        assertThat(fileStorageService.countFilesMarkedDeleted()).isEqualTo(deletedFileCountBeforeDelete + 1);
        assertThat(Files.exists(moduleRoot.resolve("original").resolve(storedFile.getRetrievalName()))).isTrue();

        mockMvc.perform(post("/admin/files/purge-deleted"))
                .andExpect(status().is3xxRedirection());

        assertThat(fileStorageService.countFilesMarkedDeleted()).isZero();
        assertThat(Files.exists(moduleRoot.resolve("original").resolve(storedFile.getRetrievalName()))).isFalse();
        assertThatThrownBy(() -> fileStorageService.open(storedFile.getFileKey()))
                .isInstanceOf(com.baseta.blobstore.file.StoredFileNotFoundException.class);
    }

    @Test
    void shouldRegenerateImageFoldersWhenModuleSizesChange() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("catalog-images");
        form.setDisplayName("Catalog Images");
        form.setProjectId(createProject("catalog-team", "Catalog Team").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80\nmedium=300x300");

        ModuleEntity module = moduleManagementService.save(form);
        Path moduleRoot = moduleRoot(module);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "catalog.jpg",
                "image/jpeg",
                createImageBytes()
        );
        StoredFileView storedFile = fileStorageService.store(module.getCode(), file);

        ModuleForm updateForm = moduleService.buildForm(module.getId());
        updateForm.setDisplayName("Catalog Assets");
        updateForm.setImageSizeDefinitions("thumb=120x120\nlarge=900x900");

        moduleManagementService.save(updateForm);

        assertThat(Files.exists(moduleRoot.resolve("original").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("thumb").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("large").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("medium"))).isFalse();

        FilePayload largePayload = fileStorageService.open(storedFile.getFileKey(), "large");
        assertThat(largePayload.getContentLength()).isPositive();
    }

    @Test
    void shouldSoftDeleteModuleAndKeepOriginalFiles() throws Exception {
        ModuleForm form = new ModuleForm();
        form.setCode("archive-images");
        form.setDisplayName("Archive Images");
        form.setProjectId(createProject("archive-team", "Archive Team").getId());
        form.setType(ModuleType.IMAGE);
        form.setImageSizeDefinitions("thumb=80x80");

        ModuleEntity module = moduleManagementService.save(form);
        Path moduleRoot = moduleRoot(module);
        StoredFileView storedFile = fileStorageService.store(
                module.getCode(),
                new MockMultipartFile("file", "archive.jpg", "image/jpeg", createImageBytes())
        );

        moduleManagementService.delete(module.getId());

        assertThat(Files.exists(moduleRoot.resolve("original").resolve(storedFile.getRetrievalName()))).isTrue();
        assertThat(Files.exists(moduleRoot.resolve("thumb"))).isFalse();
        assertThat(moduleService.findAll()).extracting(ModuleView::getCode).doesNotContain("archive-images");
        assertThat(fileStorageService.open(storedFile.getFileKey()).getContentLength()).isPositive();
        assertThatThrownBy(() -> fileStorageService.open(storedFile.getFileKey(), "thumb"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProjectEntity createProject(String code, String displayName) {
        ProjectForm form = new ProjectForm();
        form.setCode(code + "-" + System.nanoTime());
        form.setDisplayName(displayName + " " + System.nanoTime());
        return projectService.create(form);
    }

    private Path moduleRoot(ModuleEntity module) {
        return Path.of("build/test-storage").resolve(module.getStorageFolder());
    }

    private byte[] createImageBytes() throws Exception {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        return outputStream.toByteArray();
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
    }
}
