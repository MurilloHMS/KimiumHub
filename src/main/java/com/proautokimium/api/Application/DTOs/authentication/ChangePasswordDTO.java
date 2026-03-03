package com.proautokimium.api.Application.DTOs.authentication;

public record ChangePasswordDTO(String login, String currentPassword, String newPassword) { }
