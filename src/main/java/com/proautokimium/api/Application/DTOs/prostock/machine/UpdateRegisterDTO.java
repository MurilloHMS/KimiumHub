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
        String motivoAlteracaoPrevisao,
        /**
         * Pedido explícito para lançar a movimentação de estoque junto.
         *
         * Não é inferido do status de propósito: a importação de planilha usa o
         * mesmo caminho, e inferir faria 200 linhas virarem 200 movimentações.
         *
         * Primitivo, não `Boolean`: quem não manda o campo — o desktop, a
         * importação — cai em `false`, que é "não encoste no estoque".
         */
        boolean adjustStock
) { }
