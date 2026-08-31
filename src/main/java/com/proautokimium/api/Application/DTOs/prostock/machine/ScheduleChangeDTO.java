package com.proautokimium.api.Application.DTOs.prostock.machine;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma alteração de previsão, como a tela lê. */
public record ScheduleChangeDTO(
        UUID id,
        String campo,
        String valorAnterior,
        String valorNovo,
        String motivo,
        String changedBy,
        LocalDateTime changedAt
) {}
