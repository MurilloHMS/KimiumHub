package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidaturas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Candidatura extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidato candidato;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id",  nullable = false)
    private Vaga vaga;
    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_atual")
    private Etapa etapaAtual;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusCandidatura status;
    @Column(name = "criado_em")
    private LocalDateTime criado_em;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizado_em;
}
