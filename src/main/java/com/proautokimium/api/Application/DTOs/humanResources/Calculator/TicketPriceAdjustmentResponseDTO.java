package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import com.proautokimium.api.domain.enums.humanResources.TransportType;

import java.math.BigDecimal;

public record TicketPriceAdjustmentResponseDTO(
        int affectedCount,
        TransportType transportType,
        BigDecimal newTicketPrice
) {}
