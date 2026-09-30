package com.proautokimium.api.Application.DTOs.sales;

import com.proautokimium.api.domain.enums.sales.ChecklistEventType;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O checklist aberto: o resumo, o conteúdo, a linha do tempo, o que mudou em
 * cada reenvio e o que está diferente do Sankhya.
 */
public record ChecklistDetailDTO(
        ChecklistSummaryDTO summary,
        ChecklistContent content,
        List<Event> events,
        List<Change> changes,
        List<ErpDifference> erpDifferences
) {
    public record Event(ChecklistEventType type, int version, String actorLogin, String actorName,
                        String notes, LocalDateTime createdAt) {}

    public record Change(int version, String field, String before, String after,
                         String changedBy, LocalDateTime changedAt) {}

    /** "Sankhya: Vila Industrial → checklist: Centro" — o que a Controladoria atualiza no ERP. */
    public record ErpDifference(String field, String erp, String checklist) {}
}
