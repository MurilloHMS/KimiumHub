package com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate;

import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Um atestado e a conferência dele.
 *
 * {@code submittedAt} é o primeiro envio; o arquivo em vigor pode ser de um
 * reenvio ({@code resubmittedAt}). {@code resubmitDeadline} só vem quando dá
 * para reenviar agora — a tela não repete a regra do prazo.
 */
public record MedicalCertificateResponseDTO(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate startDate,
        LocalDate endDate,
        long daysCount,
        SubmissionType submissionType,
        Boolean confirmedLegible,
        String originalFilename,
        LocalDateTime submittedAt,
        MedicalCertificateStatus status,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNotes,
        LocalDateTime resubmittedAt,
        String resubmitComment,
        LocalDateTime resubmitDeadline,
        List<MedicalCertificateAttemptDTO> previousAttempts
) {
}
