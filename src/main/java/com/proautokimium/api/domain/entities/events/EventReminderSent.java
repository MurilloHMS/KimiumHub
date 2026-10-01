package com.proautokimium.api.domain.entities.events;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Um dia em que o lembrete de um evento saiu. É um fato registrado, não um
 * estado que muda: por isso não tem método que altere a linha.
 *
 * <p>A chave única (evento, dia) da V113 é o que impede um segundo envio no
 * mesmo dia — e só protege se esta linha for gravada ANTES de enviar.
 */
@Entity
@Table(name = "event_reminders_sent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventReminderSent extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompanyEvent event;

    /** O dia do envio: é por ele que a chave única barra o segundo envio. */
    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /** Quantos convidados receberam neste dia — zero quando todos já tinham respondido. */
    @Column(name = "recipients", nullable = false)
    private int recipients;

    private EventReminderSent(CompanyEvent event, LocalDate day, int recipients, LocalDateTime now) {
        this.event = event;
        this.sentOn = day;
        this.recipients = recipients;
        this.sentAt = now;
    }

    public static EventReminderSent of(CompanyEvent event, LocalDate day, int recipients, LocalDateTime now) {
        return new EventReminderSent(event, day, recipients, now);
    }
}
