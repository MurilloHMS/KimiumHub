package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da planilha, já lida e diagnosticada — e <b>ainda não gravada</b>.
 *
 * <p>Até 2026-09-24 o envio lia, casava motorista, atribuía departamento e
 * salvava tudo numa tacada, respondendo "Importação concluída com sucesso!".
 * Dois problemas ficavam invisíveis: motorista que não casava entrava calado em
 * {@code SEM_DEPARTAMENTO} — e o relatório agrupa por departamento —, e
 * reenviar a mesma planilha duplicava o mês inteiro.
 *
 * @param linha               a linha na planilha, contando como o Excel conta.
 *                            É o que permite a pessoa ir lá e corrigir a origem
 * @param departmentId        o sugerido, vindo do setor do funcionário.
 *                            <b>{@code null} quando o motorista não casou</b> —
 *                            e aí quem escolhe é quem confere, não um padrão
 * @param motoristaEncontrado se o nome da planilha achou alguém em
 *                            {@code employees}
 * @param jaExiste            mesmo motorista, mesma data e mesmo valor já
 *                            gravados. <b>Não bloqueia</b>: dois abastecimentos
 *                            idênticos no mesmo dia acontecem
 */
public record FuelSupplyPreviewRowDTO(
        int linha,
        String driverName,
        LocalDate fuelSupplyDate,
        String uf,
        String plate,
        double actualHodometer,
        String fuelType,
        double liters,
        double totalValue,
        double price,
        double diferenceHodometer,
        double averageKm,
        UUID departmentId,
        String departmentName,
        boolean motoristaEncontrado,
        boolean jaExiste
) { }
