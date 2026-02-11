package com.proautokimium.api.Application.DTOs.machine;

import java.time.LocalDateTime;
import java.util.UUID;

public record MachineMovementDTO(
        UUID id,
        LocalDateTime movementDate,
        int quantity
) { }
