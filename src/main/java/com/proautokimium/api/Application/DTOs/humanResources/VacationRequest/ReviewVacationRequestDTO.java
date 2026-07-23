package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import java.util.UUID;

public record ReviewVacationRequestDTO(
        UUID reviewerId,
        String notes
) {
}
