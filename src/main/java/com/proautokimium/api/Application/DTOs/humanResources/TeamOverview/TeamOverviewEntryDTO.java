package com.proautokimium.api.Application.DTOs.humanResources.TeamOverview;

import com.proautokimium.api.domain.enums.humanResources.AvailabilityStatus;
import com.proautokimium.api.domain.enums.humanResources.ContractType;

import java.util.UUID;

public record TeamOverviewEntryDTO(
        UUID employeeId,
        String name,
        UUID teamId,
        String teamName,
        UUID companyId,
        String companyName,
        ContractType contractType,
        AvailabilityStatus availabilityStatus
) {
}
