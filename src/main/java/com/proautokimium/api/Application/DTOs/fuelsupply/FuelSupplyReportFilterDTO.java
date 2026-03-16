package com.proautokimium.api.Application.DTOs.fuelsupply;

import com.proautokimium.api.domain.enums.ReportFormat;

public record FuelSupplyReportFilterDTO(
        int month,
        int year,
        ReportFormat format
) {}
