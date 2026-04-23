package com.baseta.blobstore.project;

import com.baseta.blobstore.module.ModuleCodeNormalizer;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    public static final String DEFAULT_PROJECT_CODE = "default";
    public static final String DEFAULT_PROJECT_NAME = "Default Project";

    private final ProjectRepository projectRepository;

    public List<ProjectView> findAll() {
        return projectRepository.findAllByDeletedAtIsNullOrderByDisplayNameAsc().stream()
                .map(ProjectView::fromEntity)
                .toList();
    }

    public ProjectEntity getById(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ProjectNotFoundException(String.valueOf(id)));
    }

    @Transactional
    public ProjectEntity ensureDefaultProject() {
        return projectRepository.findAllByDeletedAtIsNullOrderByDisplayNameAsc().stream()
                .filter(project -> DEFAULT_PROJECT_CODE.equals(project.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    ProjectForm form = new ProjectForm();
                    form.setCode(DEFAULT_PROJECT_CODE);
                    form.setDisplayName(DEFAULT_PROJECT_NAME);
                    ProjectEntity project = create(form);
                    log.info("Created default project '{}'", project.getCode());
                    return project;
                });
    }

    @Transactional
    public Long defaultProjectId() {
        List<ProjectEntity> projects = projectRepository.findAllByDeletedAtIsNullOrderByDisplayNameAsc();
        if (projects.isEmpty()) {
            return ensureDefaultProject().getId();
        }
        return projects.stream()
                .min(Comparator.comparing((ProjectEntity project) -> !DEFAULT_PROJECT_CODE.equals(project.getCode()))
                        .thenComparing(ProjectEntity::getDisplayName))
                .map(ProjectEntity::getId)
                .orElseGet(() -> ensureDefaultProject().getId());
    }

    @Transactional
    public ProjectEntity create(ProjectForm form) {
        String projectCode = ModuleCodeNormalizer.normalize(form.getCode(), "Project code");
        String displayName = form.getDisplayName().trim();

        if (projectRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(projectCode)) {
            throw new IllegalArgumentException("Project code already exists");
        }
        if (projectRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull(displayName)) {
            throw new IllegalArgumentException("Project display name already exists");
        }

        ProjectEntity project = new ProjectEntity();
        project.setCode(projectCode);
        project.setDisplayName(displayName);
        ProjectEntity savedProject = projectRepository.save(project);
        log.info("Created project '{}'", savedProject.getCode());
        return savedProject;
    }
}
