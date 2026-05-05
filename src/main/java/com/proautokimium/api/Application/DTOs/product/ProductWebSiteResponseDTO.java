package com.proautokimium.api.Application.DTOs.product;

import java.util.List;
import java.util.UUID;

public record ProductWebSiteResponseDTO(
        UUID id,
        String systemCode,
        String name,
        boolean active,
        List<String> cores,
        String finalidade,
        String diluicao,
        String descricao
) {
}
