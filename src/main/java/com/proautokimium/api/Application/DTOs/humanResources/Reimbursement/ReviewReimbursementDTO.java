package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

import java.util.UUID;

public record ReviewReimbursementDTO(
        UUID reviewerId,
        String notes
) {
}
