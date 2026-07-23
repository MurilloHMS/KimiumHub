package com.proautokimium.api.Application.DTOs.humanResources.Team;

import com.proautokimium.api.domain.entities.humanResources.Department;

import java.util.UUID;

public record TeamResponseDTO(UUID id, String name, Department department) {
}
