package com.proautokimium.api.Application.DTOs.signature;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSignatureDTO(
        @NotBlank
        String nome,
        @NotBlank
        String cargo,
        Email email,
        @NotBlank
        String celular,
        @NotBlank
        String whatsapp,
        @NotBlank
        String site

) { }
