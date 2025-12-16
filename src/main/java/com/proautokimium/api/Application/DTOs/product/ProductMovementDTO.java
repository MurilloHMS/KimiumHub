package com.proautokimium.api.Application.DTOs.product;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductMovementDTO(LocalDateTime movementDate,
                                 int quantity,
                                 String systemCode) {
}
