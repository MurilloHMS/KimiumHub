package com.proautokimium.api.Application.DTOs.product;

import java.time.LocalDate;

public record ProductMovementDTO(LocalDate movementDate,
                                 int quantity,
                                 String systemCode) {
}
