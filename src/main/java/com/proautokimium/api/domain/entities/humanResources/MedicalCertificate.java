package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

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
    }

    public static MedicalCertificate submit(Employee employee, LocalDate startDate, LocalDate endDate,
                                             SubmissionType submissionType, Boolean confirmedLegible,
                                             String originalFilename, String storagePath, LocalDateTime submittedAt) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Data final não pode ser antes da data inicial");
        }
        if (submissionType == SubmissionType.PHOTO && !Boolean.TRUE.equals(confirmedLegible)) {
            throw new IllegalArgumentException("É preciso confirmar que a foto está legível antes de enviar");
        }
        return new MedicalCertificate(employee, startDate, endDate, submissionType, confirmedLegible,
                originalFilename, storagePath, submittedAt);
    }

    public long getDaysCount() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
