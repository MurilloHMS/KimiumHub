package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
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
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;


    // Methods
    public void iniciar() {
        this.etapaAtual = Etapa.TRIAGEM;
        this.status = StatusCandidatura.EM_ANDAMENTO;
        this.criadoEm = LocalDateTime.now();
    }

    public void avancarEtapa() {

        if (this.status == StatusCandidatura.REPROVADO ||
                this.status == StatusCandidatura.ENCERRADO) {
            throw new IllegalStateException("Candidatura finalizada");
        }

        switch (this.etapaAtual) {
            case TRIAGEM:
                this.etapaAtual = Etapa.ENTREVISTA_RH;
                break;
            case ENTREVISTA_RH:
                this.etapaAtual = Etapa.PROPOSTA;
                break;
            case PROPOSTA:
                this.aprovar();
                break;
            case CONTRATADO:
                throw new IllegalStateException("Já está na última etapa");
            default:
                throw new IllegalStateException("Etapa inválida");
        }

        this.atualizadoEm = LocalDateTime.now();
    }

    public void aprovar() {
        this.status = StatusCandidatura.APROVADO;
        this.etapaAtual = Etapa.CONTRATADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void reprovar(){
        this.status = StatusCandidatura.REPROVADO;
    }

    public void encerrar(){
        this.status = StatusCandidatura.ENCERRADO;
    }

    // Converters

    public ResponseCandidaturaDTO toDTO() {
        return new ResponseCandidaturaDTO(
                this.getId(),
                this.getCandidato().getNome(),
                this.getCandidato().getEmail().getAddress(),
                this.getCandidato().getTelefone(),
                this.getCandidato().getUrlLinkedin(),
                this.getCandidato().getPathCurriculo(),
                this.getVaga().getTitulo(),
                this.getEtapaAtual(),
                this.getStatus(),
                this.getCriadoEm(),
                this.getAtualizadoEm()
        );
    }
}
