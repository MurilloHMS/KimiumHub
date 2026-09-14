package com.proautokimium.api.domain.enums.events;

/** Onde a palestra acontece. */
public enum TalkLocationType {
    /** No local do evento — o padrão, e o caso de quase toda palestra. */
    EVENT,
    /** Noutra empresa do grupo. */
    COMPANY,
    /** Num endereço digitado: um cliente, um kartódromo. */
    ADDRESS
}
