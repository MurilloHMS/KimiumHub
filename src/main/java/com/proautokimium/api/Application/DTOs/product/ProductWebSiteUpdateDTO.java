package com.proautokimium.api.Application.DTOs.product;

import java.util.List;

public record ProductWebSiteUpdateDTO(
        String name,
        boolean active,
        List<String> cores,
        String finalidade,
        String diluicao,
        String descricao
) {
}
