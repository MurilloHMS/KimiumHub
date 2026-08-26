package com.proautokimium.api.Application.DTOs.prostock.machine;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Um lançamento de estoque de máquina, junto com o que ele significa na
 * programação.
 *
 * `delta` e não quantidade absoluta, ao contrário do resto do estoque: o
 * servidor precisa saber QUANTAS entraram ou saíram para conferir se a conta
 * fecha com as programações escolhidas.
 */
public record ReconcileDTO(
        @NotBlank(message = "Código do produto é obrigatório")
        String systemCode,

        /** Positivo entra, negativo sai. Zero é recusado. */
        int delta,

        LocalDateTime movementDate,

        /** Quais programações viram ENTREGUE. Só quando `delta` é negativo. */
        List<UUID> registersToDeliver,

        /** Quantas programações nascem DISPONIVEL. Só quando `delta` é positivo. */
        Integer registersToCreate
) {}
