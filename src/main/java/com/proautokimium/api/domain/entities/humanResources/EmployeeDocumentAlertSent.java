package com.proautokimium.api.domain.entities.humanResources;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aviso de vencimento que já saiu — a trava contra aviso repetido.
 *
 * A chave é documento + vencimento + marco (`UNIQUE` na V110). O vencimento
 * entra de propósito: se o RH corrigir a data, os avisos voltam a valer para a
 * data nova. `daysBefore = -1` é o aviso do próprio dia.
 */
@jakarta.persistence.Entity
@Table(name = "employee_document_alerts_sent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeDocumentAlertSent extends com.proautokimium.api.domain.abstractions.Entity {

    /** O marco do aviso no dia do vencimento. */
    public static final int ON_DUE_DATE = -1;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public EmployeeDocumentAlertSent(UUID documentId, LocalDate dueDate, int daysBefore, LocalDateTime sentAt) {
        this.documentId = documentId;
        this.dueDate = dueDate;
        this.daysBefore = daysBefore;
        this.sentAt = sentAt;
    }
}
