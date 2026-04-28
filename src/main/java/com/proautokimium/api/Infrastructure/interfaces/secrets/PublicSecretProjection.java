package com.proautokimium.api.Infrastructure.interfaces.secrets;

import java.time.LocalDateTime;

public interface PublicSecretProjection {
    byte[] getEncryptedContent();
    byte[] getIv();
    byte[] getAuthTag();
    LocalDateTime getExpiresAt();
}
