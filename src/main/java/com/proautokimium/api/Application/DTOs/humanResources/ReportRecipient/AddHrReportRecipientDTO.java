package com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient;

import jakarta.validation.constraints.NotBlank;

public record AddHrReportRecipientDTO(@NotBlank(message = "Informe o e-mail") String email) {}
