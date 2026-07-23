package com.proautokimium.api.Application.DTOs.humanResources.Team;

import com.proautokimium.api.domain.entities.humanResources.Department;

public record CreateTeamRequestDTO(String name, Department department) {
}
