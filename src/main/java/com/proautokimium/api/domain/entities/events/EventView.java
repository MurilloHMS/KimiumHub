package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.entities.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Quem abriu o evento: uma linha por convidado, com a primeira e a última vez
 * e quantas vezes. Só convidado conta — o organizador conferindo não passa por
 * aqui (a regra mora no serviço).
 */
@Entity
@Table(name = "event_views")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventView extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompanyEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "first_viewed_at", nullable = false)
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    private EventView(CompanyEvent event, Employee employee, LocalDateTime now) {
        this.event = event;
        this.employee = employee;
        this.firstViewedAt = now;
        this.lastViewedAt = now;
        this.viewCount = 1;
    }

    /** A primeira vez que esta pessoa abre este evento. */
    public static EventView first(CompanyEvent event, Employee employee, LocalDateTime now) {
        return new EventView(event, employee, now);
    }

    /** Abriu de novo: a mesma linha, mais uma vez no contador. */
    public void seenAgain(LocalDateTime now) {
        this.lastViewedAt = now;
        this.viewCount++;
    }

}