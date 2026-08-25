package com.proautokimium.api.domain.entities.prostock.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma alteração de previsão de saída, com o motivo.
 *
 * Imutável por natureza: é registro do que aconteceu, e não tem update. Por
 * isso carrega só `@CreatedBy` e `@CreatedDate` — não existe "quem alterou o
 * histórico".
 *
 * Só nasce quando **já havia** uma data. Preencher a previsão pela primeira vez
 * é completar cadastro, não adiar, e por isso `previsaoAnterior` é NOT NULL.
 */
@Entity
@Table(name = "machine_register_schedule_changes")
@EntityListeners(AuditingEntityListener.class)
public class MachineScheduleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "register_id", nullable = false)
    private MachineRegister register;

    @Column(name = "previsao_anterior", nullable = false)
    private LocalDateTime previsaoAnterior;

    /** Nulo quando alguém **apaga** a previsão em vez de trocar por outra. */
    @Column(name = "previsao_nova")
    private LocalDateTime previsaoNova;

    @Column(name = "motivo", length = 500, nullable = false)
    private String motivo;

    @CreatedBy
    @Column(name = "changed_by", length = 120, updatable = false)
    private String changedBy;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected MachineScheduleChange() {}

    public MachineScheduleChange(MachineRegister register,
                                 LocalDateTime previsaoAnterior,
                                 LocalDateTime previsaoNova,
                                 String motivo) {
        this.register = register;
        this.previsaoAnterior = previsaoAnterior;
        this.previsaoNova = previsaoNova;
        this.motivo = motivo;
    }

    public UUID getId() { return id; }
    public MachineRegister getRegister() { return register; }
    public LocalDateTime getPrevisaoAnterior() { return previsaoAnterior; }
    public LocalDateTime getPrevisaoNova() { return previsaoNova; }
    public String getMotivo() { return motivo; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
