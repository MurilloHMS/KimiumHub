package com.proautokimium.api.Application.DTOs.product;

import java.util.List;
import java.util.UUID;

public record ProductWebSiteCreateDTO(
        String systemCode,
        String name,
        boolean active,
        List<String> cores,
        String finalidade,
        String diluicao,
        String concentracao,
        String localUso,
        String descricao,
        UUID equipmentId
) {
}
