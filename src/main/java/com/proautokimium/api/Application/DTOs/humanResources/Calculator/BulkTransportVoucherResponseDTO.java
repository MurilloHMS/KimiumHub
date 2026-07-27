package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BulkTransportVoucherResponseDTO(
        String companyName,
        UUID companyId,
        List<BulkTransportVoucherResultDTO> employees,
        BigDecimal grandTotal
) {}
