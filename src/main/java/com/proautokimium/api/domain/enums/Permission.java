package com.proautokimium.api.domain.enums;

/**
 * O que se pode fazer dentro de uma tela.
 *
 * As sete do Sankhya, menos o "Repassar" — descartado no planejamento porque
 * era o único que ninguém conseguia explicar sem exemplo.
 *
 * A ordem daqui é a ordem das colunas no grid de configuração. Reordenar
 * "para ficar alfabético" muda a tela.
 */
public enum Permission {
    ALTERAR,
    EXCLUIR,
    CONSULTAR,
    CONFIGURAR,
    INCLUIR,
    ENVIAR,
    BAIXAR
}
