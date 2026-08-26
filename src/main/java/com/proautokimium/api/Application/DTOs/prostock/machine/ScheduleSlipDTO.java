package com.proautokimium.api.Application.DTOs.prostock.machine;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um adiamento, com de quem ele é.
 *
 * O `ScheduleChangeDTO` já existe, mas serve à outra pergunta: "o histórico
 * DESTA linha". Aqui a linha é o que precisa ser dito — quem lê está olhando o
 * conjunto e não sabe de qual programação cada adiamento veio.
 */
public record ScheduleSlipDTO(
        UUID registerId,
        String nomeCliente,
        String machineName,
        LocalDateTime previsaoAnterior,
        LocalDateTime previsaoNova,
        String motivo,
        LocalDateTime changedAt
) {}
