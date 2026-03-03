package com.proautokimium.api.Application.DTOs.authentication;

public record ResetPasswordDTO(String token, String newPassword) {
}
