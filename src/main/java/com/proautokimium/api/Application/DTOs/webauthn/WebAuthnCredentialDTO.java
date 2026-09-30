package com.proautokimium.api.Application.DTOs.webauthn;

import com.proautokimium.api.domain.entities.auth.WebAuthnCredential;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aparelho, como o Perfil e o cadastro do funcionário mostram.
 *
 * @param credentialId público por natureza (o navegador o manda em todo login);
 *                     é com ele que o site marca "Você está nele"
 */
public record WebAuthnCredentialDTO(UUID id, String credentialId, String deviceLabel,
                                    LocalDateTime createdAt, LocalDateTime lastUsedAt) {

    public static WebAuthnCredentialDTO from(WebAuthnCredential c) {
        return new WebAuthnCredentialDTO(c.getId(), c.getCredentialId(), c.getDeviceLabel(),
                c.getCreatedAt(), c.getLastUsedAt());
    }
}
