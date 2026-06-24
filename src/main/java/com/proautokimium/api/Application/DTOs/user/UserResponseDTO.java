package com.proautokimium.api.Application.DTOs.user;

import com.proautokimium.api.domain.enums.UserRole;

import java.util.Collection;

/** codParceiro é o código do funcionário vinculado a este usuário, ou null se não houver vínculo. */
public record UserResponseDTO(String login, Collection<UserRole> roles, String codParceiro) {
}
