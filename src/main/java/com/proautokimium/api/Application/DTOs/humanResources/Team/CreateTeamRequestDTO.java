package com.proautokimium.api.Application.DTOs.humanResources.Team;

import com.proautokimium.api.domain.entities.humanResources.Department;

import java.util.UUID;

public record CreateTeamRequestDTO(String name, UUID departmentId) {
}
