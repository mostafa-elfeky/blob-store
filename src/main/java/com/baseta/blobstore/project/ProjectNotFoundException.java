package com.baseta.blobstore.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String value) {
        super("Project not found: " + value);
    }
}
