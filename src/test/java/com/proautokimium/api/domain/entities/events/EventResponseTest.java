package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.enums.events.EventAnswer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** A resposta ao convite: mudar atualiza, e a primeira resposta continua registrada. */
class EventResponseTest {

    static final LocalDateTime DIA_20 = LocalDateTime.of(2026, 9, 20, 10, 0);
    static final LocalDateTime DIA_30 = LocalDateTime.of(2026, 9, 30, 17, 45);

    @Test
    @DisplayName("a primeira resposta grava as duas datas iguais")
    void firstAnswer() {
        EventResponse r = EventResponse.first(null, null, EventAnswer.GOING, "Só no primeiro dia", DIA_20);

        assertThat(r.getAnswer()).isEqualTo(EventAnswer.GOING);
        assertThat(r.getFirstAnsweredAt()).isEqualTo(DIA_20);
        assertThat(r.getAnsweredAt()).isEqualTo(DIA_20);
    }

    @Test
    @DisplayName("mudar de ideia atualiza a resposta e a data, e a primeira continua lá")
    void changingKeepsTheFirstDate() {
        EventResponse r = EventResponse.first(null, null, EventAnswer.GOING, null, DIA_20);

        r.change(EventAnswer.NOT_GOING, "Surgiu uma viagem", DIA_30);

        assertThat(r.getAnswer()).isEqualTo(EventAnswer.NOT_GOING);
        assertThat(r.getAnsweredAt()).isEqualTo(DIA_30);
        assertThat(r.getFirstAnsweredAt()).as("quando respondeu pela primeira vez").isEqualTo(DIA_20);
    }

    @Test
    @DisplayName("observação só de espaços vira vazia; a de verdade perde os espaços das pontas")
    void noteIsCleaned() {
        assertThat(EventResponse.first(null, null, EventAnswer.GOING, "   ", DIA_20).getNote()).isNull();
        assertThat(EventResponse.first(null, null, EventAnswer.GOING, null, DIA_20).getNote()).isNull();
        assertThat(EventResponse.first(null, null, EventAnswer.GOING, "  Vegetariano  ", DIA_20).getNote())
                .isEqualTo("Vegetariano");
    }
}
