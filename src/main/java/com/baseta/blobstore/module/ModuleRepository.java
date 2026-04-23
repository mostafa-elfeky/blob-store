package com.baseta.blobstore.module;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<ModuleEntity, Long> {

    List<ModuleEntity> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<ModuleEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    Optional<ModuleEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);
}
