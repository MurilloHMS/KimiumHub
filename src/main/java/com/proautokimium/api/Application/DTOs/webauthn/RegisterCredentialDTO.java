package com.proautokimium.api.Application.DTOs.webauthn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** A resposta de {@code navigator.credentials.create()}, com os binários em base64url. */
public record RegisterCredentialDTO(
        @NotNull UUID challengeId,
        @NotBlank String clientDataJSON,
        @NotBlank String attestationObject,
        List<String> transports
) {}
