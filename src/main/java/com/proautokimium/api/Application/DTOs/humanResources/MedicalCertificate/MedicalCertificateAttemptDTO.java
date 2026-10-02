package com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate;

import com.proautokimium.api.domain.enums.humanResources.SubmissionType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Um arquivo recusado e substituído, com a recusa que levou. */
public record MedicalCertificateAttemptDTO(
        UUID id,
        SubmissionType submissionType,
        String originalFilename,
        LocalDateTime submittedAt,
        String comment,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
}
