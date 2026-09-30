package com.proautokimium.api.Application.DTOs.webauthn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** A resposta de {@code navigator.credentials.get()}, com os binários em base64url. */
public record AuthenticateCredentialDTO(
        @NotNull UUID challengeId,
        @NotBlank String credentialId,
        @NotBlank String clientDataJSON,
        @NotBlank String authenticatorData,
        @NotBlank String signature,
        String userHandle
) {}
