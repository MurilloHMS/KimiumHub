package com.proautokimium.api.domain.enums;

/**
 * O código do ERP já existe aqui, e como o quê.
 *
 * <p>Desde a V101 o `cod_parceiro` é único em `parceiros`, então um código que
 * já é de alguém não pode virar funcionário — o insert bateria no índice. Melhor
 * dizer isso no formulário, antes de a pessoa preencher empresa, setor, cargo,
 * nível, contrato e data de admissão para levar erro no fim.
 */
public enum PartnerConflict {
    /** Já é cliente. Uma pessoa pode ser as duas coisas, mas não com o mesmo código. */
    ALREADY_A_CUSTOMER,
    /** Já é funcionário: este cadastro já existe. */
    ALREADY_AN_EMPLOYEE
}
