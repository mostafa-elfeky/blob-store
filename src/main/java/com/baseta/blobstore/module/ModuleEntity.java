package com.baseta.blobstore.module;

import com.baseta.blobstore.project.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class ModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String displayName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuleType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VideoType videoType;

    @Column(nullable = false)
    private String storageFolder;

    @Column(nullable = false)
    private boolean publicAccess;

    private Integer maxFileSizeMb;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "module_image_sizes", joinColumns = @JoinColumn(name = "module_id"))
    private List<ImageSizeDefinition> imageSizes = new ArrayList<>();

    private Integer originalImageWidth;

    private Integer originalImageHeight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "module_supported_media_types", joinColumns = @JoinColumn(name = "module_id"))
    @Column(name = "media_type", nullable = false, length = 150)
    private List<String> supportedMediaTypes = new ArrayList<>();

    private Integer maxVideoDurationSeconds;

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt = Instant.now();

    private Instant deletedAt;

    public void setImageSizes(List<ImageSizeDefinition> imageSizes) {
        this.imageSizes = new ArrayList<>(imageSizes);
    }

    public void setSupportedMediaTypes(List<String> supportedMediaTypes) {
        this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
    }
}
