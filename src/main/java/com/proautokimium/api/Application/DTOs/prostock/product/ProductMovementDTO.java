package com.proautokimium.api.Application.DTOs.prostock.product;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * `movementDate` é quando a movimentação aconteceu; `createdAt`, quando foi
 * registrada.
 *
 * A tela precisa das duas: mostra a primeira e **ordena pela segunda**.
 * `movement_date` é `date`, sem hora, então dois lançamentos do mesmo dia
 * empatam e a ordem fica por conta do acaso. Ver a V83.
 */
public record ProductMovementDTO(LocalDateTime movementDate,
                                 OffsetDateTime createdAt,
                                 int quantity,
                                 String systemCode) {
}
