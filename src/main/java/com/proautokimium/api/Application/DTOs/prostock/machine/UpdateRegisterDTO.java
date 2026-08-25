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
        String tecnico,
        String regiao,
        String consultor,

        /**
         * Obrigatório só quando a previsão **muda** e já havia data.
         *
         * Preencher pela primeira vez não é adiamento — é completar cadastro, e
         * cobrar justificativa ali só ensina a digitar "ok" para passar da tela.
         */
        String motivoAlteracaoPrevisao
) { }
