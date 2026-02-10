package com.proautokimium.api.Application.DTOs.machine;

import java.time.LocalDateTime;

public record MachineMovementDTO(
        LocalDateTime movementDate,
        int quantity
) { }
