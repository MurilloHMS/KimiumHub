package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidaturas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Candidatura extends com.proautokimium.api.domain.abstractions.Entity{
    private UUID candidatoID;
    private UUID vagaID;
    private Etapa etapaAtual;
    private StatusCandidatura status;
    private LocalDateTime criado_em;
    private LocalDateTime atualizado_em;
}
