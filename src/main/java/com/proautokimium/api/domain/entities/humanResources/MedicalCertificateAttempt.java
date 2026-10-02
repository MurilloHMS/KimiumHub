package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Um arquivo que o RH recusou e a pessoa substituiu, com a recusa que levou.
 *
 * Imutável: é a trilha do atestado. Nasce só pelo
 * {@link MedicalCertificate#resubmit}, que é quem sabe o que estava em vigor.
 */
@jakarta.persistence.Entity
@Table(name = "medical_certificate_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalCertificateAttempt extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private MedicalCertificate certificate;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 10)
    private SubmissionType submissionType;

    @Column(name = "confirmed_legible")
    private Boolean confirmedLegible;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "comment", length = 500)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500, nullable = false)
    private String reviewNotes;

    MedicalCertificateAttempt(MedicalCertificate certificate, SubmissionType submissionType,
                              Boolean confirmedLegible, String originalFilename, String storagePath,
                              LocalDateTime submittedAt, String comment,
                              Employee reviewedBy, LocalDateTime reviewedAt, String reviewNotes) {
        this.certificate = certificate;
        this.submissionType = submissionType;
        this.confirmedLegible = confirmedLegible;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.submittedAt = submittedAt;
        this.comment = comment;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNotes = reviewNotes;
    }
}
