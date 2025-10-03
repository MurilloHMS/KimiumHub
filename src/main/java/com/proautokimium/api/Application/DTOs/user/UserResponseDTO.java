package com.proautokimium.api.Application.DTOs.user;

import com.proautokimium.api.domain.enums.UserRole;

import java.util.Collection;

public record UserResponseDTO(String login, Collection<UserRole> Roles) {
}
