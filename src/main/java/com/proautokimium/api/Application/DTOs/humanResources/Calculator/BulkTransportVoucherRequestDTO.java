package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import com.proautokimium.api.domain.enums.humanResources.TransportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BulkTransportVoucherRequestDTO(
        @NotNull TransportType transportType,
        @NotNull @Min(1) Integer workingDays
) {}
