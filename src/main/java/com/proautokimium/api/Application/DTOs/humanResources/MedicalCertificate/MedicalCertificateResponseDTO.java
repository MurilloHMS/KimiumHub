package com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate;

import com.proautokimium.api.domain.enums.humanResources.SubmissionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalCertificateResponseDTO(
        UUID id,
        UUID employeeId,
        LocalDate startDate,
        LocalDate endDate,
        long daysCount,
        SubmissionType submissionType,
        Boolean confirmedLegible,
        String originalFilename,
        LocalDateTime submittedAt
) {
}
