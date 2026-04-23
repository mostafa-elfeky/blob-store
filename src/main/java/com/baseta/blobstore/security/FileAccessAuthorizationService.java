package com.baseta.blobstore.security;

import com.baseta.blobstore.file.StoredFileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("fileAccessAuthorizationService")
@RequiredArgsConstructor
public class FileAccessAuthorizationService {

    private final StoredFileRepository storedFileRepository;

    public boolean canReadFile(UUID fileKey, Authentication authentication) {
        return storedFileRepository.findByFileKey(fileKey)
                .map(file -> file.getModule().isPublicAccess() || isAuthenticated(authentication))
                .orElse(false);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
