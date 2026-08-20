package com.proautokimium.api.Application.DTOs.holerite;

import jakarta.validation.constraints.NotBlank;

public record CancelarHoleriteDTO(@NotBlank String motivo) { }