package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record BulkTransportVoucherResultDTO(
        UUID employeeId,
        String employeeName,
        String document,
        Integer dailyTicketCount,
        BigDecimal ticketPrice,
        Integer workingDays,
        BigDecimal totalAmount
) {}
