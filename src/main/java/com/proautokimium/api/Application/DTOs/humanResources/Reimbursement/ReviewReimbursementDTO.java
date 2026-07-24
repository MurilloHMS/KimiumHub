package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

/** Quem revisa é sempre o RH autenticado — reviewerId nunca vem do cliente. */
public record ReviewReimbursementDTO(
        String notes
) {
}
