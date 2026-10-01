package com.proautokimium.api.domain.entities.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quando o evento começa — o prazo para responder ao convite e para o
 * lembrete parar. O primeiro dia, na hora da primeira palestra DESSE dia.
 */
class CompanyEventStartsAtTest {

    static final LocalDate DIA_1 = LocalDate.of(2026, 10, 6);
    static final LocalDate DIA_2 = LocalDate.of(2026, 10, 7);

    static CompanyEvent evento(EventTalk... palestras) {
        CompanyEvent e = new CompanyEvent();
        e.setStartDate(DIA_1);
        e.setEndDate(DIA_2);
        for (EventTalk t : palestras) e.getTalks().add(t);
        return e;
    }

    static EventTalk palestra(LocalDate dia, int hora) {
        EventTalk t = new EventTalk();
        t.setDate(dia);
        t.setStartTime(LocalTime.of(hora, 0));
        return t;
    }

    @Test
    @DisplayName("começa na palestra mais cedo do primeiro dia, mesmo fora de ordem")
    void earliestTalkOfTheFirstDay() {
        CompanyEvent e = evento(palestra(DIA_1, 14), palestra(DIA_1, 8), palestra(DIA_1, 10));

        assertThat(e.startsAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 8, 0));
    }

    @Test
    @DisplayName("palestra de outro dia não conta, nem se for mais cedo")
    void otherDaysDoNotCount() {
        CompanyEvent e = evento(palestra(DIA_2, 7), palestra(DIA_1, 9));

        assertThat(e.startsAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 9, 0));
    }

    @Test
    @DisplayName("sem palestra no primeiro dia, começa à meia-noite dele")
    void noTalkOnTheFirstDayStartsAtMidnight() {
        assertThat(evento(palestra(DIA_2, 14)).startsAt()).isEqualTo(DIA_1.atStartOfDay());
        assertThat(evento().startsAt()).isEqualTo(DIA_1.atStartOfDay());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("remindsOn: do dia start - N em diante; sem N, sempre")
    void remindsOnWindow() {
        CompanyEvent e = new CompanyEvent();
        e.setStartDate(java.time.LocalDate.of(2026, 10, 6));

        org.assertj.core.api.Assertions.assertThat(e.remindsOn(java.time.LocalDate.of(2026, 9, 1))).as("sem N").isTrue();

        e.setReminderDaysBefore(3);
        org.assertj.core.api.Assertions.assertThat(e.remindsOn(java.time.LocalDate.of(2026, 10, 2))).isFalse();
        org.assertj.core.api.Assertions.assertThat(e.remindsOn(java.time.LocalDate.of(2026, 10, 3))).isTrue();
        org.assertj.core.api.Assertions.assertThat(e.remindsOn(java.time.LocalDate.of(2026, 10, 5))).isTrue();
    }
}
