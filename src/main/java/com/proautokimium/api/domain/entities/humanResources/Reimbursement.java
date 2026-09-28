package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    // ── Contestação: uma só, até 30 dias depois da recusa (V109) ───────────────
    // O pedido é o mesmo e volta a PENDING. O que a primeira análise decidiu vai
    // para first_*, e o comprovante anterior para original_receipt_*.

    /** Prazo para contestar, contado da recusa. */
    public static final int CONTEST_WINDOW_DAYS = 30;

    /** Não nulo = já contestou. É o que impede a segunda vez. */
    @Column(name = "contested_at")
    private LocalDateTime contestedAt;

    @Column(name = "contest_comment", length = 500)
    private String contestComment;

    @Column(name = "original_receipt_filename", length = 255)
    private String originalReceiptFilename;

    @Column(name = "original_receipt_storage_path", length = 500)
    private String originalReceiptStoragePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_reviewed_by_id")
    private Employee firstReviewedBy;

    @Column(name = "first_reviewed_at")
    private LocalDateTime firstReviewedAt;

    @Column(name = "first_review_notes", length = 500)
    private String firstReviewNotes;

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
            throw new InvalidRequestDataException("Valor do reembolso precisa ser maior que zero");
        }
        return new Reimbursement(employee, expenseDate, amount, category, reason,
                receiptOriginalFilename, receiptStoragePath, requestedAt);
    }

    public void approve(Employee reviewer, String notes, LocalDateTime now) {
        if (status != ReimbursementStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível aprovar um reembolso pendente");
        }
        ensureNotOwnRequest(reviewer);
        this.status = ReimbursementStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    public void reject(Employee reviewer, String notes, LocalDateTime now) {
        if (status != ReimbursementStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível reprovar um reembolso pendente");
        }
        ensureNotOwnRequest(reviewer);
        if (notes == null || notes.isBlank()) {
            throw new InvalidRequestDataException("Motivo é obrigatório ao reprovar");
        }
        this.status = ReimbursementStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = now;
    }

    /** Registra o pagamento — separado de approve() porque a data pode não ser conhecida na hora de aprovar. */
    public void pay(LocalDate paymentDate, LocalDateTime now) {
        if (status != ReimbursementStatus.APPROVED) {
            throw new InvalidStatusTransitionException("Só é possível pagar um reembolso aprovado");
        }
        this.paymentDate = paymentDate;
        this.paidAt = now;
        this.status = ReimbursementStatus.PAID;
    }

    /**
     * Contesta a recusa com um comprovante novo e um comentário.
     *
     * Só o comprovante muda — valor, data e categoria continuam, e o RH analisa
     * a mesma despesa de novo. Decidido em 2026-09-28: uma vez só, e até
     * {@value #CONTEST_WINDOW_DAYS} dias depois da recusa.
     */
    public void contest(String receiptFilename, String receiptStoragePath, String comment, LocalDateTime now) {
        if (status != ReimbursementStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Só é possível contestar um reembolso recusado");
        }
        if (contestedAt != null) {
            throw new InvalidStatusTransitionException("Este reembolso já foi contestado. A segunda recusa é final.");
        }
        LocalDateTime deadline = contestDeadline();
        if (deadline == null || now.isAfter(deadline)) {
            throw new InvalidStatusTransitionException("O prazo para contestar terminou em "
                    + (deadline == null ? "—" : deadline.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
        if (comment == null || comment.isBlank()) {
            throw new InvalidRequestDataException("Explique o que mudou no comprovante");
        }
        if (receiptStoragePath == null || receiptStoragePath.isBlank()) {
            throw new InvalidRequestDataException("Anexe o novo comprovante");
        }

        this.originalReceiptFilename = this.receiptOriginalFilename;
        this.originalReceiptStoragePath = this.receiptStoragePath;
        this.receiptOriginalFilename = receiptFilename;
        this.receiptStoragePath = receiptStoragePath;

        this.firstReviewedBy = this.reviewedBy;
        this.firstReviewedAt = this.reviewedAt;
        this.firstReviewNotes = this.reviewNotes;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewNotes = null;

        this.contestComment = comment.strip();
        this.contestedAt = now;
        this.status = ReimbursementStatus.PENDING;
    }

    /** Até quando dá para contestar esta recusa; nulo quando não é o caso. */
    public LocalDateTime contestDeadline() {
        if (status != ReimbursementStatus.REJECTED || contestedAt != null || reviewedAt == null) return null;
        return reviewedAt.plusDays(CONTEST_WINDOW_DAYS);
    }

    public boolean canContest(LocalDateTime now) {
        LocalDateTime deadline = contestDeadline();
        return deadline != null && !now.isAfter(deadline);
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
