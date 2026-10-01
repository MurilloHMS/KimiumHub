package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Um evento da empresa: a Poseidon Week, de 22 a 25.
 *
 * <p>{@code Company} no nome porque {@code Event} sozinho briga com metade das
 * classes do Spring ({@code ApplicationEvent}, {@code @EventListener}) na hora de
 * importar.
 *
 * <p><b>Os dias saem do intervalo</b> {@link #startDate} a {@link #endDate}; não
 * há tabela de dias.
 *
 * <p><b>{@link #publishedAt} nulo é rascunho</b>, e rascunho não aparece em
 * Documentos.
 */
@jakarta.persistence.Entity
@Table(name = "company_events")
@Getter
@Setter
@NoArgsConstructor
public class CompanyEvent extends Entity {

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20)
    private EventLocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "place_name", length = 150)
    private String placeName;

    @Embedded
    private Address address;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, startTime ASC")
    private List<EventTalk> talks = new ArrayList<>();

    // ── Convidados e lembrete (V113) ─────────────────────────────────────────

    /** Verdadeiro: todos os funcionários ativos. Falso: a soma das três listas abaixo. */
    @Column(name = "audience_all", nullable = false)
    private boolean audienceAll = true;

    @ManyToMany
    @JoinTable(name = "company_event_audience_companies",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "company_id"))
    private Set<Company> audienceCompanies = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "company_event_audience_departments",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> audienceDepartments = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "company_event_audience_employees",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    private Set<Employee> audienceEmployees = new HashSet<>();

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = false;

    /** A hora do lembrete diário; só vale com {@link #reminderEnabled} ligado. */
    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    /** Quantos dias antes do primeiro dia o lembrete começa (V114); obrigatório com o lembrete ligado. */
    @Column(name = "reminder_days_before")
    private Integer reminderDaysBefore;

    public boolean isPublished() {
        return publishedAt != null;
    }

    /** Inclusivo nas duas pontas: a palestra do último dia está dentro. */
    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Se o lembrete já entrou na janela neste dia: do dia {@code startDate - N}
     * em diante. Sem N (evento de antes da V114), vale desde a publicação.
     */
    public boolean remindsOn(LocalDate day) {
        return reminderDaysBefore == null || !day.isBefore(startDate.minusDays(reminderDaysBefore));
    }

    /**
     * Quando o evento começa: o primeiro dia, na hora da primeira palestra
     * desse dia; sem palestra nesse dia, à meia-noite. Até este instante dá
     * para responder ao convite, e até ele o lembrete é enviado.
     */
    public LocalDateTime startsAt(){
        LocalTime firstTalk = talks.stream()
                .filter(t -> startDate.equals(t.getDate()))
                .map(EventTalk::getStartTime)
                .min(Comparator.naturalOrder())
                .orElse(LocalTime.MIDNIGHT);
        return startDate.atTime(firstTalk);
    }
}
