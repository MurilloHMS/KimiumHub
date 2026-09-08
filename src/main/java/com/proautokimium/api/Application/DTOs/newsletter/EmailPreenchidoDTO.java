package com.proautokimium.api.Application.DTOs.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailPreenchidoDTO(
        @NotBlank String codigoCliente,
        @NotBlank @Email String email
) {
}
