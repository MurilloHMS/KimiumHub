package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha que a pessoa <b>escolheu</b> gravar, já com o departamento decidido.
 *
 * <p>A tela devolve o que conferiu, e não o arquivo de novo: o que vai para o
 * banco é exatamente o que estava na tela quando ela apertou o botão. Reler a
 * planilha aqui abriria espaço para gravar algo que ela não viu.
 *
 * @param linha        a linha na planilha, que veio da conferência e volta só
 *                     para as mensagens de recusa apontarem para algum lugar
 * @param departmentId obrigatório, mesmo para quem o casamento sugeriu. Deixar
 *                     a API escolher um padrão é o defeito que esta tela existe
 *                     para corrigir
 */
public record FuelSupplyImportRowDTO(
        int linha,
        LocalDate fuelSupplyDate,
        String uf,
        String plate,
        String driverName,
        UUID departmentId,
        double actualHodometer,
        double diferenceHodometer,
        double averageKm,
        String fuelType,
        double liters,
        double price,
        double totalValue
) { }
