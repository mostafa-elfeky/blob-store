package com.baseta.blobstore.project;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectView {

    private final Long id;
    private final String code;
    private final String displayName;

    public static ProjectView fromEntity(ProjectEntity entity) {
        return new ProjectView(entity.getId(), entity.getCode(), entity.getDisplayName());
    }
}
