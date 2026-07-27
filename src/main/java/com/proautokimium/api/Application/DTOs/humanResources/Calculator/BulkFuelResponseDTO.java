package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BulkFuelResponseDTO(
        String companyName,
        UUID companyId,
        List<BulkFuelResultDTO> employees,
        BigDecimal grandTotal
) {}
