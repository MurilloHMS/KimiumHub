package com.proautokimium.api.Application.DTOs.humanResources.Hierarchy;

import java.util.UUID;

public record HierarchyResponseDTO(UUID id, String name, Integer levelOrder) {
}
