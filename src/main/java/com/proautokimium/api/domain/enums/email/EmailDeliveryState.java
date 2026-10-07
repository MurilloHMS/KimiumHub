package com.proautokimium.api.domain.enums.email;

/**
 * O que se sabe da chegada de um e-mail ao destinatário, pelo relatório do
 * SMTP Locaweb. "Enviado" (status SENT) é a Locaweb ter aceitado; a entrega é
 * o servidor do destinatário ter aceitado.
 */
public enum EmailDeliveryState {
    /** Ainda não saiu (na fila ou falhou): não há entrega a saber. */
    NOT_SENT,
    /** Saiu antes do rastreio existir, sem o cabeçalho X-SMTPLW. */
    UNTRACKED,
    /** Saiu há pouco; o agendador ainda pergunta à Locaweb. */
    AWAITING,
    /** Passou a janela sem a Locaweb confirmar: o agendador parou de perguntar. */
    UNCONFIRMED,
    DELIVERED,
    /** Devolvido pelo servidor do destinatário depois de a Locaweb aceitar. */
    BOUNCED
}
