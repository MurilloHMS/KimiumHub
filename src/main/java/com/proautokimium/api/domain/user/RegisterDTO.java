package com.proautokimium.api.domain.user;

import com.proautokimium.api.domain.enums.UserRole;

public record RegisterDTO(String login, String password, UserRole role) {}
