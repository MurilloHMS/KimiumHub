package com.proautokimium.api.Application.DTOs.machine;

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
        @NotBlank(message = "O tipo de máquina é obrigatório")
        MachineType machineType,
        @NotBlank(message = "O status da máquina é obrigatório")
        MachineStatus machineStatus,
        int minimum_stock,
        boolean active
) { }
