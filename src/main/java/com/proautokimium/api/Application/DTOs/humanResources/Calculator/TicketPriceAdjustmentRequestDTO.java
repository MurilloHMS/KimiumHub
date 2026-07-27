package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import com.proautokimium.api.domain.enums.humanResources.TransportType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TicketPriceAdjustmentRequestDTO(
        @NotNull TransportType transportType,
        @NotNull @DecimalMin("0.01") BigDecimal newTicketPrice
) {}
