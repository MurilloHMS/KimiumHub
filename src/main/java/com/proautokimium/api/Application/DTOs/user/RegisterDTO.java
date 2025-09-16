package com.proautokimium.api.Application.DTOs.user;

import com.proautokimium.api.domain.enums.UserRole;

import java.util.List;

public record RegisterDTO(String login, String password, List<UserRole> roles) {}
