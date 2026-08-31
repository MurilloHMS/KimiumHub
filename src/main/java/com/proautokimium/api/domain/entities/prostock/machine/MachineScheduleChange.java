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

    @Column(name = "campo", length = 40, nullable = false)
    private String campo;

    @Column(name = "valor_anterior", length = 500)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 500)
    private String valorNovo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @CreatedBy
    @Column(name = "changed_by", length = 120, updatable = false)
    private String changedBy;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected MachineScheduleChange() {}

    public MachineScheduleChange(MachineRegister register,
                                 String campo,
                                 String valorAnterior,
                                 String valorNovo,
                                 String motivo) {
        this.register = register;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.motivo = motivo;
    }

    public UUID getId() { return id; }
    public MachineRegister getRegister() { return register; }
    public String getCampo() { return campo; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public String getMotivo() { return motivo; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
