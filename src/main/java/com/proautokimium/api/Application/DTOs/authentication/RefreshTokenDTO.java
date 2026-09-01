package com.proautokimium.api.Application.DTOs.authentication;

import jakarta.validation.constraints.NotBlank;

/** O refresh token que o navegador guardou, voltando para ser trocado. */
public record RefreshTokenDTO(@NotBlank String refreshToken) {}
