package com.proautokimium.api.domain.enums.humanResources;

/**
 * A situação de um documento do funcionário.
 *
 * Calculada, nunca gravada: depende de "hoje", e uma coluna de status ficaria
 * errada à meia-noite do dia em que o documento vence, até alguém a atualizar.
 */
public enum EmployeeDocumentStatus {
    VALID,
    EXPIRING,
    EXPIRED,
    NO_DUE_DATE,
    REPLACED
}
