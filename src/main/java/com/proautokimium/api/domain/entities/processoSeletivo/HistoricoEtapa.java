package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private UUID candidaturaID;
    private Etapa etapaAnterior;
    private Etapa etapaNova;
    private StatusCandidatura status;
    private String observacao;
    private UUID responsavelID;
    private LocalDateTime dataMovimentacao;
}
