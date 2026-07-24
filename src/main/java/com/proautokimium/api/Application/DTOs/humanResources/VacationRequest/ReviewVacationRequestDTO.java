package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

/** Quem revisa é sempre o RH autenticado — reviewerId nunca vem do cliente. */
public record ReviewVacationRequestDTO(
        String notes
) {
}
