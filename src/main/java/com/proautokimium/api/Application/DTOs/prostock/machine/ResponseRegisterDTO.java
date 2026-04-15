package com.proautokimium.api.Application.DTOs.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseRegisterDTO(
        UUID id,
        UUID machineId,
        String nomeCliente,
        short tag,
        String solicitante,
        MachineStatus status,
        String Observacao,
        LocalDateTime previsaoEntrega,
        String tecnico
) { }
