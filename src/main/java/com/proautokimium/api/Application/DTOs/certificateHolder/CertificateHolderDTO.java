package com.proautokimium.api.Application.DTOs.certificateHolder;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.validation.constraints.NotBlank;

public record CertificateHolderDTO(
        @NotBlank(message = "Nome é obrigatório")
        String name,
        @NotBlank(message = "Celular é obrigatório")
        String cellphone,
        @NotBlank(message = "Email é obrigatório")
        Email email) {}
