package com.proautokimium.api.Application.DTOs.newsletter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * O período pedido pela tela.
 *
 * **Mês e ano, e não intervalo de datas.** A newsletter é mensal, e assim somem
 * sozinhas as perguntas de início depois do fim e de mês pela metade — quem
 * deriva o primeiro e o último dia é a API, de um lugar só.
 */
public record PreviaRequestDTO(
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotNull @Min(2000) @Max(2100) Integer ano
) {
}
