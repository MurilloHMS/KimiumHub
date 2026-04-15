package com.proautokimium.api.Application.DTOs.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record MachineDTO(
        UUID id,
        String systemCode,
        @NotBlank(message = "O Nome é obrigatório")
        String name,
        @NotBlank(message = "A marca é obrigatória")
        String brand,
        MachineType machineType,
        MachineStatus machineStatus,
        int minimum_stock,
        boolean active
) { }
