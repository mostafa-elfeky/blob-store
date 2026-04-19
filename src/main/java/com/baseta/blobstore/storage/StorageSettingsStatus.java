package com.baseta.blobstore.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StorageSettingsStatus {

    private final String activeRootDir;
    private final String savedRootDir;
    private final boolean restartRequired;
}
