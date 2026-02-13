package com.proautokimium.api.Application.DTOs.machine;

import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record MachineMovementDTO(
        UUID id,
        @NotBlank(message = "A data de Movimentação é obrigatória")
        LocalDateTime movementDate,
        @NegativeOrZero(message = "Insira um número maior que zero")
        @NotBlank(message = "A quantidade é obrigatória")
        int quantity
) { }
