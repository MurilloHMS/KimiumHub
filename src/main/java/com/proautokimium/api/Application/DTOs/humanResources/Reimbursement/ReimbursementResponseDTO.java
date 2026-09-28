package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReimbursementResponseDTO(
        UUID id,
        UUID employeeId,
        LocalDate expenseDate,
        BigDecimal amount,
        String category,
        String reason,
        String receiptOriginalFilename,
        ReimbursementStatus status,
        LocalDateTime requestedAt,
        UUID reviewedById,
        LocalDateTime reviewedAt,
        String reviewNotes,
        LocalDate paymentDate,
        LocalDateTime paidAt,
        // Contestação (V109). `contestDeadline` só vem quando ainda dá para
        // contestar — é o que a tela usa para mostrar o botão e o "até 28/10".
        LocalDateTime contestedAt,
        String contestComment,
        String originalReceiptFilename,
        UUID firstReviewedById,
        LocalDateTime firstReviewedAt,
        String firstReviewNotes,
        LocalDateTime contestDeadline
) {
}
