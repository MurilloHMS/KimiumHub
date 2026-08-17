package com.proautokimium.api.Application.DTOs.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientInviteDTO(@NotBlank @Email String email) {
}
