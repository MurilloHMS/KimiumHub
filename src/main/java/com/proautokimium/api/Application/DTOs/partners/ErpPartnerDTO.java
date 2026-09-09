package com.proautokimium.api.Application.DTOs.partners;

import com.proautokimium.api.domain.enums.PartnerConflict;

/**
 * Um parceiro do Sankhya, para preencher o formulário de funcionário.
 *
 * <p>O ERP sabe nome, documento e e-mail. Empresa, setor, cargo, nível, tipo de
 * contrato e data de admissão continuam sendo de quem cadastra — são justamente
 * os campos obrigatórios do {@code CreateEmployeeRequestDTO}.
 *
 * @param email     pode vir {@code null}, e isso é legítimo: muitos funcionários
 *                  não têm e-mail no ERP, e é por isso que o primeiro acesso é
 *                  por CPF
 * @param conflict  {@code null} quando o código está livre
 */
public record ErpPartnerDTO(
        String codParceiro,
        String name,
        String document,
        String email,
        boolean activeInErp,
        PartnerConflict conflict,
        /** Nome de quem já usa o código, para a tela dizer de quem se trata. */
        String conflictWith
) { }
