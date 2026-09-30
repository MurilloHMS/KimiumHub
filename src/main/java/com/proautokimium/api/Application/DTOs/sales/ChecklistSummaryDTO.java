package com.proautokimium.api.Application.DTOs.sales;

import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Uma linha da lista — do vendedor ou da Controladoria. */
public record ChecklistSummaryDTO(
        UUID id,
        Long number,
        String sellerLogin,
        String sellerName,
        Integer customerCode,
        String customerName,
        String customerDocument,
        boolean newCustomer,
        ChecklistStatus status,
        int version,
        boolean hasOrder,
        BigDecimal orderTotal,
        boolean filledOffline,
        LocalDateTime firstSubmittedAt,
        LocalDateTime lastSubmittedAt,
        String reviewNotes,
        LocalDateTime reviewedAt,
        String changeReason,
        LocalDateTime changeRequestedAt
) {
    public static ChecklistSummaryDTO from(Checklist c) {
        return new ChecklistSummaryDTO(c.getId(), c.getNumber(), c.getSellerLogin(), c.getSellerName(),
                c.getCustomerCode(), c.getCustomerName(), c.getCustomerDocument(), c.isNewCustomer(),
                c.getStatus(), c.getVersion(), c.isHasOrder(), c.getOrderTotal(), c.isFilledOffline(),
                c.getFirstSubmittedAt(), c.getLastSubmittedAt(), c.getReviewNotes(), c.getReviewedAt(),
                c.getChangeReason(), c.getChangeRequestedAt());
    }
}
