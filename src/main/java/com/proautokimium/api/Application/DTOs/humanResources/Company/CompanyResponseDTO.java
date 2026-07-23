package com.proautokimium.api.Application.DTOs.humanResources.Company;

import java.util.UUID;

public record CompanyResponseDTO(UUID id, String name, String legalName, String cnpj) {
}
