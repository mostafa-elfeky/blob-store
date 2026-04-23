package com.baseta.blobstore.web;

import com.baseta.blobstore.file.StoredFileNotFoundException;
import com.baseta.blobstore.module.ModuleNotFoundException;
import com.baseta.blobstore.project.ProjectNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ModuleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleModuleNotFound(ModuleNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFileNotFound(StoredFileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
