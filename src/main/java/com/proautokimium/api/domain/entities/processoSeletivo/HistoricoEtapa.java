package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_etapas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HistoricoEtapa extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;
    private Etapa etapaAnterior;
    private Etapa etapaNova;
    private StatusCandidatura status;
    private String observacao;
    private LocalDateTime dataMovimentacao;
}
