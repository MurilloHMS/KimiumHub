package com.proautokimium.api.Application.DTOs.holerite;

import java.util.List;

public record VincularHoleriteResultDTO(
        int totalPaginas,
        int vinculados,
        List<String> naoEncontrados
) {
}
