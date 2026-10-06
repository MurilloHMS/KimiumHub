package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma solicitação, com os contadores das respostas. Na lista do RH é isto que
 * aparece em cada linha; no rascunho os contadores são zero.
 */
public record DocumentRequestDTO(
        UUID id,
        String title,
        String instructions,
        LocalDate dueDate,
        RequestStatus status,
        List<RequestField> form,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        LocalDateTime closedAt,
        Counts counts
) {
    public record Counts(long total, long pending, long submitted, long approved, long returned) {
        public static final Counts NONE = new Counts(0, 0, 0, 0, 0);
    }
}
