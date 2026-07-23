package com.proautokimium.api.domain.entities.humanResources;

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
            throw new IllegalArgumentException("Data final não pode ser antes da data inicial");
        }
        return new VacationRequest(employee, startDate, endDate, replacementEmployee, requestedAt);
    }

    public long getDaysRequested() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public void approve(Employee reviewer, String notes, LocalDateTime now) {
        if (status != VacationRequestStatus.PENDING) {
            throw new IllegalStateException("Só é possível aprovar uma solicitação pendente");
        }
        this.status = VacationRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    public void reject(Employee reviewer, String notes, LocalDateTime now) {
        if (status != VacationRequestStatus.PENDING) {
            throw new IllegalStateException("Só é possível reprovar uma solicitação pendente");
        }
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Motivo é obrigatório ao reprovar");
        }
        this.status = VacationRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }
}
