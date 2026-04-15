package com.proautokimium.api.Application.DTOs.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;

import java.time.LocalDateTime;

public record UpdateRegisterDTO(
        String nomeCliente,
        short tag,
        String solicitante,
        MachineStatus status,
        String Observacao,
        LocalDateTime previsaoEntrega,
        String tecnico
) { }
