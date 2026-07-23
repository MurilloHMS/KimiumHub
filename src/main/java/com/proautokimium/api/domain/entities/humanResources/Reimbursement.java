package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "reimbursements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reimbursement extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "receipt_original_filename", length = 255)
    private String receiptOriginalFilename;

    @Column(name = "receipt_storage_path", length = 500, nullable = false)
    private String receiptStoragePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReimbursementStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    private Reimbursement(Employee employee, LocalDate expenseDate, BigDecimal amount, String category,
                           String reason, String receiptOriginalFilename, String receiptStoragePath,
                           LocalDateTime requestedAt) {
        this.employee = employee;
        this.expenseDate = expenseDate;
        this.amount = amount;
        this.category = category;
        this.reason = reason;
        this.receiptOriginalFilename = receiptOriginalFilename;
        this.receiptStoragePath = receiptStoragePath;
        this.status = ReimbursementStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static Reimbursement request(Employee employee, LocalDate expenseDate, BigDecimal amount, String category,
                                         String reason, String receiptOriginalFilename, String receiptStoragePath,
                                         LocalDateTime requestedAt) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Valor do reembolso precisa ser maior que zero");
        }
        return new Reimbursement(employee, expenseDate, amount, category, reason,
                receiptOriginalFilename, receiptStoragePath, requestedAt);
    }

    public void approve(Employee reviewer, String notes, LocalDateTime now) {
        if (status != ReimbursementStatus.PENDING) {
            throw new IllegalStateException("Só é possível aprovar um reembolso pendente");
        }
        this.status = ReimbursementStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    public void reject(Employee reviewer, String notes, LocalDateTime now) {
        if (status != ReimbursementStatus.PENDING) {
            throw new IllegalStateException("Só é possível reprovar um reembolso pendente");
        }
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Motivo é obrigatório ao reprovar");
        }
        this.status = ReimbursementStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    /** Registra o pagamento — separado de approve() porque a data pode não ser conhecida na hora de aprovar. */
    public void pay(LocalDate paymentDate, LocalDateTime now) {
        if (status != ReimbursementStatus.APPROVED) {
            throw new IllegalStateException("Só é possível pagar um reembolso aprovado");
        }
        this.paymentDate = paymentDate;
        this.paidAt = now;
        this.status = ReimbursementStatus.PAID;
    }
}
