package com.baseta.blobstore.file;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFileEntity, Long> {

    List<StoredFileEntity> findAllByModuleId(Long moduleId);

    List<StoredFileEntity> findAllByModuleDeletedAtIsNotNull();

    Optional<StoredFileEntity> findByFileKey(UUID fileKey);

    long countByModuleDeletedAtIsNotNull();

    List<StoredFileEntity> findTop20ByModuleDeletedAtIsNullOrderByCreatedAtDesc();
}
