package com.proautokimium.api.Application.DTOs.user;

import jakarta.validation.constraints.NotBlank;

/** Vincula um usuário a um funcionário (parceiro) pelo código do parceiro. */
public record LinkEmployeeRequest(@NotBlank String codParceiro) {
}
