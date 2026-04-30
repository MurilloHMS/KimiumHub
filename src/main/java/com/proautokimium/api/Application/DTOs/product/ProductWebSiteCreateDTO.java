package com.proautokimium.api.Application.DTOs.product;

import java.util.List;

public record ProductWebSiteCreateDTO(
        String systemCode,
        String name,
        boolean active,
        List<String> cores,
        String finalidade,
        String diluicao,
        String descricao
) {
}
