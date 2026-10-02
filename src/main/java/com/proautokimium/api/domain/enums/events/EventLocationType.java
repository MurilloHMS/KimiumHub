package com.proautokimium.api.domain.enums.events;

/**
 * Onde o evento acontece.
 *
 * <p>Nulo também vale: rascunho pode ainda não saber o lugar.
 */
public enum EventLocationType {
    /** Uma empresa do grupo; o endereço é lido do cadastro dela. */
    COMPANY,
    /** Um endereço digitado, com nome do lugar. */
    ADDRESS,
    /**
     * Uma transmissão: live no Instagram, YouTube, LinkedIn, Meet… O evento
     * guarda o link e o horário, e o colaborador responde "Estou ciente".
     */
    ONLINE
}
