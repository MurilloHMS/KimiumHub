package com.proautokimium.api.Application.DTOs.machine;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;

import java.util.UUID;

public record MachineDTO(
        UUID id,
        String systemCode,
        String name,
        String brand,
        MachineType machineType,
        MachineStatus machineStatus,
        int minimum_stock,
        boolean active
) { }
