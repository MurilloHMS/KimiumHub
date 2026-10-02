package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.events.EventAnswer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A resposta de um convidado: vai ou não vai, com uma observação opcional.
 * Uma por pessoa por evento — mudar a resposta atualiza esta mesma linha, e a
 * primeira resposta continua registrada.
 */
@Entity
@Table(name = "event_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventResponse extends com.proautokimium.api.domain.abstractions.Entity {

    public static final int NOTE_MAX = 500;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompanyEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer", nullable = false, length = 20)
    private EventAnswer answer;

    @Column(name = "note", length = NOTE_MAX)
    private String note;

    @Column(name = "first_answered_at", nullable = false)
    private LocalDateTime firstAnsweredAt;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    private EventResponse(CompanyEvent event, Employee employee, LocalDateTime now) {
        this.event = event;
        this.employee = employee;
        this.firstAnsweredAt = now;
    }

    /** A primeira resposta desta pessoa a este evento. */
    public static EventResponse first(CompanyEvent event, Employee employee, EventAnswer answer, String note, LocalDateTime now){
        EventResponse response = new EventResponse(event, employee, now);
        response.change(answer, note, now);
        return response;
    }

    /** Muda a resposta, ou só a observação. */
    public void change(EventAnswer answer, String note, LocalDateTime now){
        this.answer = answer;
        this.note = note == null || note.isBlank() ? null : note.strip();
        this.answeredAt = now;
    }
}
