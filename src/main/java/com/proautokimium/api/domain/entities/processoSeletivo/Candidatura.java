package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
        if(this.status == StatusCandidatura.EM_ANDAMENTO || this.status == StatusCandidatura.APROVADO)
            throw new IllegalStateException("Não é possível iniciar uma candidatura já em andamento ou aprovada.");

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
        if(this.etapaAtual == Etapa.CONTRATADO || this.status == StatusCandidatura.APROVADO)
            throw new IllegalStateException("Não é possível reprovar uma candidatura contratada.");

        this.status = StatusCandidatura.REPROVADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void encerrar(){
        if(this.etapaAtual == Etapa.CONTRATADO || this.status == StatusCandidatura.APROVADO || this.status == StatusCandidatura.ENCERRADO)
            throw new IllegalStateException("Não é possível encerrar uma candidatura aprovada ou já encerrada.");

        this.status = StatusCandidatura.ENCERRADO;
        this.atualizadoEm = LocalDateTime.now();
    }
}
