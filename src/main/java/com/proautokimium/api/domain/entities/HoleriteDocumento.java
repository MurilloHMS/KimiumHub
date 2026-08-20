package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.HoleriteTipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holerite_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoleriteDocumento extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate competencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private HoleriteTipo tipo;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by_id")
    private User canceledBy;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private User replacedBy;


    public HoleriteDocumento(Employee employee, LocalDate competencia, HoleriteTipo tipo,
                             String originalFilename, String storagePath) {
        this.employee = employee;
        this.competencia = competencia;
        this.tipo = tipo;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * A PRIMEIRA abertura, não a última.
     *
     * Para auditoria de entrega é a primeira que prova alguma coisa; sobrescrever
     * a cada download apagaria justamente o dado que interessa.
     */
    public void marcarAberto(LocalDateTime agora) {
        if (this.openedAt == null) this.openedAt = agora;
    }

    /** Idempotente: confirmar duas vezes não muda a data do recibo. */
    public void confirmar(LocalDateTime agora) {
        if (this.confirmedAt == null) this.confirmedAt = agora;
    }

    public void cancelar(User quem, String motivo, LocalDateTime agora) {
        this.canceledAt = agora;
        this.canceledBy = quem;
        this.cancelReason = motivo;
    }

    /**
     * Trocar o arquivo zera o recibo.
     *
     * Sem isto a auditoria afirmaria "visualizado em 05/11" sobre um documento
     * que a pessoa nunca viu — ela viu o anterior.
     */
    public void substituirArquivo(String storagePath, String originalFilename, User quem, LocalDateTime agora) {
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.replacedAt = agora;
        this.replacedBy = quem;
        this.openedAt = null;
        this.confirmedAt = null;
    }
}
