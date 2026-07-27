package com.proautokimium.api.Application.DTOs.humanResources.Team;

import com.proautokimium.api.Application.DTOs.humanResources.Department.DepartmentResponseDTO;

import java.util.UUID;

public record TeamResponseDTO(UUID id, String name, DepartmentResponseDTO department) {
}
