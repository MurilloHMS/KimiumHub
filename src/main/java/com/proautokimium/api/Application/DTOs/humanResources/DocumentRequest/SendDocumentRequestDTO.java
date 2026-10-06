package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.util.Set;
import java.util.UUID;

/** O público, como nos Eventos: todos, ou a soma das empresas, dos setores e das pessoas. */
public record SendDocumentRequestDTO(boolean all, Set<UUID> companyIds, Set<UUID> departmentIds, Set<UUID> employeeIds) {
}
