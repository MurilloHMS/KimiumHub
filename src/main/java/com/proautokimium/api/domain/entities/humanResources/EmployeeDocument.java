package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "employee_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocument extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private EmployeeDocumentType type;

    /** Opcional: documento sem vencimento (contrato por prazo indeterminado, RG). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** O documento que tomou o lugar deste. Preenchido = substituído. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private EmployeeDocument replacedBy;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    /** O LOGIN de quem vinculou — registro, não FK (sobrevive ao usuário apagado). */
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /**
     * A situação no dia informado. Recebe "hoje" em vez de chamar o relógio: a
     * regra fica testável sem Spring nem Clock, com qualquer data.
     *
     * A ordem importa: substituído vence tudo (um ASO velho vencido não é
     * pendência — já existe um novo), e sem data não tem como vencer.
     */

    public EmployeeDocumentStatus statusOn(LocalDate today) {
        if (replacedBy != null) return EmployeeDocumentStatus.REPLACED;
        if (dueDate == null) return EmployeeDocumentStatus.NO_DUE_DATE;
        if (dueDate.isBefore(today)) return EmployeeDocumentStatus.EXPIRED;

        int window = type != null ? type.warningWindowDays() : EmployeeDocumentType.DEFAULT_WARNING_DAYS;
        return daysUntilDue(today) <= window ? EmployeeDocumentStatus.EXPIRING : EmployeeDocumentStatus.VALID;
    }

    /** Dias até o vencimento: negativo já venceu, zero vence hoje. */
    public long daysUntilDue(LocalDate today) {
        return java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
    }

    /**
     * Este documento é substituído pelo mais novo — e para de gerar aviso.
     * Só entre documentos do MESMO funcionário: substituir o ASO do João pelo da
     * Maria deixaria o do João "resolvido" sem existir.
     */
    public void replaceWith(EmployeeDocument newer, LocalDateTime now) {
        if (newer == null || newer == this) {
            throw new InvalidRequestDataException("Documento substituto inválido.");
        }
        if (!getEmployee().getId().equals(newer.getEmployee().getId())) {
            throw new InvalidRequestDataException("Só dá para substituir um documento do mesmo funcionário.");
        }
        if (replacedBy != null) {
            throw new InvalidRequestDataException("Este documento já foi substituído.");
        }
        this.replacedBy = newer;
        this.replacedAt = now;
    }
}
