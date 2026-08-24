package com.proautokimium.api.Application.DTOs.certificate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CertificateBatchDTO(
        @NotEmpty(message = "Envie pelo menos um nome")
        @Size(max = 200, message = "Máximo de 200 nomes por lote")
        List<@NotBlank(message = "A lista tem um nome em branco") String> names
) {
}
