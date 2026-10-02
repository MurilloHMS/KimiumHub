package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "medical_certificates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalCertificate extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 10)
    private SubmissionType submissionType;

    @Column(name = "confirmed_legible")
    private Boolean confirmedLegible;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    /** O primeiro envio. O arquivo atual pode ser de um reenvio: ver {@link #resubmittedAt}. */
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // ── Conferência do RH (V118) ───────────────────────────────────────────────

    /** Prazo para reenviar, contado de cada recusa. */
    public static final int RESUBMIT_WINDOW_DAYS = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MedicalCertificateStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    /** Quando chegou o arquivo atual, se ele é um reenvio. */
    @Column(name = "resubmitted_at")
    private LocalDateTime resubmittedAt;

    @Column(name = "resubmit_comment", length = 500)
    private String resubmitComment;

    /** Os arquivos recusados, do mais antigo ao mais novo. */
    @OneToMany(mappedBy = "certificate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("submittedAt ASC")
    private List<MedicalCertificateAttempt> previousAttempts = new ArrayList<>();

    private MedicalCertificate(Employee employee, LocalDate startDate, LocalDate endDate,
                                SubmissionType submissionType, Boolean confirmedLegible,
                                String originalFilename, String storagePath, LocalDateTime submittedAt) {
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.submissionType = submissionType;
        this.confirmedLegible = confirmedLegible;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.submittedAt = submittedAt;
        this.status = MedicalCertificateStatus.PENDING;
    }

    public static MedicalCertificate submit(Employee employee, LocalDate startDate, LocalDate endDate,
                                             SubmissionType submissionType, Boolean confirmedLegible,
                                             String originalFilename, String storagePath, LocalDateTime submittedAt) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestDataException("Data final não pode ser antes da data inicial");
        }
        if (submissionType == SubmissionType.PHOTO && !Boolean.TRUE.equals(confirmedLegible)) {
            throw new InvalidRequestDataException("É preciso confirmar que a foto está legível antes de enviar");
        }
        return new MedicalCertificate(employee, startDate, endDate, submissionType, confirmedLegible,
                originalFilename, storagePath, submittedAt);
    }

    public long getDaysCount() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** O RH confirma que recebeu. Observação opcional. */
    public void confirmReceipt(Employee reviewer, String notes, LocalDateTime now) {
        if (status != MedicalCertificateStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível confirmar um atestado em conferência");
        }
        ensureNotOwnCertificate(reviewer);
        this.status = MedicalCertificateStatus.RECEIVED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes == null || notes.isBlank() ? null : notes.strip();
        this.reviewedAt = now;
    }

    /** O RH recusa, dizendo o porquê — é o que a pessoa lê para saber o que reenviar. */
    public void reject(Employee reviewer, String notes, LocalDateTime now) {
        if (status != MedicalCertificateStatus.PENDING) {
            throw new InvalidStatusTransitionException("Só é possível recusar um atestado em conferência");
        }
        ensureNotOwnCertificate(reviewer);
        if (notes == null || notes.isBlank()) {
            throw new InvalidRequestDataException("Diga o motivo da recusa: é o que a pessoa lê para reenviar");
        }
        this.status = MedicalCertificateStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes.strip();
        this.reviewedAt = now;
    }

    /**
     * A pessoa reenvia o atestado recusado com um arquivo novo.
     *
     * Período e dias continuam: o RH confere o mesmo afastamento de novo. O
     * arquivo anterior e a recusa que ele levou vão para a trilha. Decidido em
     * 2026-10-02: quantas vezes precisar, até {@value #RESUBMIT_WINDOW_DAYS}
     * dias depois de cada recusa. O comentário é opcional — o arquivo novo já
     * é a resposta.
     */
    public void resubmit(SubmissionType submissionType, Boolean confirmedLegible, String originalFilename,
                         String storagePath, String comment, LocalDateTime now) {
        if (status != MedicalCertificateStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Só é possível reenviar um atestado recusado");
        }
        LocalDateTime deadline = resubmitDeadline();
        if (deadline == null || now.isAfter(deadline)) {
            throw new InvalidStatusTransitionException("O prazo para reenviar terminou em "
                    + (deadline == null ? "—" : deadline.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
        if (storagePath == null || storagePath.isBlank()) {
            throw new InvalidRequestDataException("Anexe o novo atestado");
        }
        if (submissionType == SubmissionType.PHOTO && !Boolean.TRUE.equals(confirmedLegible)) {
            throw new InvalidRequestDataException("É preciso confirmar que a foto está legível antes de enviar");
        }

        previousAttempts.add(new MedicalCertificateAttempt(this, this.submissionType, this.confirmedLegible,
                this.originalFilename, this.storagePath, currentFileSubmittedAt(), this.resubmitComment,
                this.reviewedBy, this.reviewedAt, this.reviewNotes));

        this.submissionType = submissionType;
        this.confirmedLegible = confirmedLegible;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.resubmittedAt = now;
        this.resubmitComment = comment == null || comment.isBlank() ? null : comment.strip();

        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewNotes = null;
        this.status = MedicalCertificateStatus.PENDING;
    }

    /** Até quando dá para reenviar; nulo quando não está recusado. */
    public LocalDateTime resubmitDeadline() {
        if (status != MedicalCertificateStatus.REJECTED || reviewedAt == null) return null;
        return reviewedAt.plusDays(RESUBMIT_WINDOW_DAYS);
    }

    public boolean canResubmit(LocalDateTime now) {
        LocalDateTime deadline = resubmitDeadline();
        return deadline != null && !now.isAfter(deadline);
    }

    /** Quando chegou o arquivo que está em vigor — o primeiro envio ou o último reenvio. */
    public LocalDateTime currentFileSubmittedAt() {
        return resubmittedAt != null ? resubmittedAt : submittedAt;
    }

    public List<MedicalCertificateAttempt> getPreviousAttempts() {
        return Collections.unmodifiableList(previousAttempts);
    }

    /** O mesmo critério do reembolso: referência ou o mesmo id não nulo. */
    private void ensureNotOwnCertificate(Employee reviewer) {
        if (reviewer == null) return;
        boolean samePerson = reviewer == employee
                || (reviewer.getId() != null && reviewer.getId().equals(employee.getId()));
        if (samePerson) {
            throw new SelfReviewException();
        }
    }
}
