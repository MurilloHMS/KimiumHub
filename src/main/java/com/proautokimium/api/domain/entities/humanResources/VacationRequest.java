package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@jakarta.persistence.Entity
@Table(name = "vacation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VacationRequest extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_employee_id")
    private Employee replacementEmployee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VacationRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    private VacationRequest(Employee employee, LocalDate startDate, LocalDate endDate,
                             Employee replacementEmployee, LocalDateTime requestedAt) {
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.replacementEmployee = replacementEmployee;
        this.status = VacationRequestStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static VacationRequest request(Employee employee, LocalDate startDate, LocalDate endDate,
                                           Employee replacementEmployee, LocalDateTime requestedAt) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestDataException("Data final não pode ser antes da data inicial");
        }
        return new VacationRequest(employee, startDate, endDate, replacementEmployee, requestedAt);
    }

    public long getDaysRequested() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public void approve(Employee reviewer, String notes, LocalDateTime now) {
        if (status != VacationRequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível aprovar uma solicitação pendente");
        }
        ensureNotOwnRequest(reviewer);
        this.status = VacationRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    public void reject(Employee reviewer, String notes, LocalDateTime now) {
        if (status != VacationRequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível reprovar uma solicitação pendente");
        }
        ensureNotOwnRequest(reviewer);
        if (notes == null || notes.isBlank()) {
            throw new InvalidRequestDataException("Motivo é obrigatório ao reprovar");
        }
        this.status = VacationRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    /**
     * Ninguém revisa o próprio pedido. Compara por referência OU pelo mesmo id
     * não nulo — não por {@code equals}: ele compara só o id, e dois
     * funcionários ainda sem id (null == null) seriam "a mesma pessoa". O id
     * é o que pega o dono carregado como proxy do Hibernate.
     *
     * Revisor nulo (conta sem funcionário vinculado) não tem com quem comparar.
     */
    private void ensureNotOwnRequest(Employee reviewer) {
        if (reviewer == null) return;
        boolean samePerson = reviewer == employee
                || (reviewer.getId() != null && reviewer.getId().equals(employee.getId()));
        if (samePerson) {
            throw new SelfReviewException();
        }
    }
}
