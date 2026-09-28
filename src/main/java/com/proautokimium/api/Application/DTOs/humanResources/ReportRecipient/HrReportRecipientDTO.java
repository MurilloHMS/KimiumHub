package com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient;

import com.proautokimium.api.domain.entities.humanResources.HrReportRecipient;

import java.time.LocalDateTime;
import java.util.UUID;

public record HrReportRecipientDTO(UUID id, String email, LocalDateTime createdAt, String createdBy) {
    public static HrReportRecipientDTO from(HrReportRecipient r) {
        return new HrReportRecipientDTO(r.getId(), r.getEmail(), r.getCreatedAt(), r.getCreatedBy());
    }
}
