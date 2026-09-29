package com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument;

import java.util.List;
import java.util.UUID;

/** Os dias vêm do maior para o menor ("30, 7"), a ordem em que os avisos chegam. */
public record EmployeeDocumentTypeDTO(
        UUID id,
        String name,
        List<Integer> alertDaysBefore,
        boolean notifyOnExpiry,
        List<UUID> recipientEmployeeIds,
        boolean active
) {
}
