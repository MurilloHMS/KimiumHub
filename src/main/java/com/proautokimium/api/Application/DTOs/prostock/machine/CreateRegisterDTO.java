package com.proautokimium.api.Application.DTOs.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateRegisterDTO(
        UUID machineId,
        String nomeCliente,
        String tag,
        String solicitante,
        MachineStatus status,
        String Observacao,
        LocalDateTime previsaoEntrega,
        String tecnico,
        String regiao,
        String consultor,

        /** Ver `UpdateRegisterDTO.adjustStock`. */
        boolean adjustStock
) { }
