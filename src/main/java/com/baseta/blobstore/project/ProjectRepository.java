package com.baseta.blobstore.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findAllByDeletedAtIsNullOrderByDisplayNameAsc();

    Optional<ProjectEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByDisplayNameIgnoreCaseAndDeletedAtIsNull(String displayName);
}
