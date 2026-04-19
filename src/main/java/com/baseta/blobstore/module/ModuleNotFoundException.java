package com.baseta.blobstore.module;

public class ModuleNotFoundException extends RuntimeException {

    public ModuleNotFoundException(String code) {
        super("Module not found: " + code);
    }
}
