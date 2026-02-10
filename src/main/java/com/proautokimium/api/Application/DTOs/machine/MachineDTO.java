package com.proautokimium.api.Application.DTOs.machine;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;

import java.util.UUID;

public record MachineDTO(
        UUID id,
        String name,
        String brand,
        MachineType type,
        MachineStatus status,
        int minimum_stock,
        boolean active
) { }
