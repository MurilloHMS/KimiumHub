package com.proautokimium.api.domain.enums.events;

/**
 * A resposta ao convite.
 *
 * <p>Evento presencial aceita {@link #GOING} e {@link #NOT_GOING}; evento online
 * aceita só {@link #ACKNOWLEDGED} — numa live de comunicado, "não vou" não faz
 * sentido, e o que interessa saber é quem viu o aviso.
 */
public enum EventAnswer {
    GOING,
    NOT_GOING,
    /** "Estou ciente", das lives. */
    ACKNOWLEDGED;

    public boolean isPresenceAnswer() {
        return this == GOING || this == NOT_GOING;
    }
}
