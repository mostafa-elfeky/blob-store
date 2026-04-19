package com.baseta.blobstore.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationLogEntry {

    private final String timestamp;
    private final String level;
    private final String logger;
    private final String message;
}
