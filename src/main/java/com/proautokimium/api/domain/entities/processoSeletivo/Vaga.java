package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vagas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Vaga  extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "titulo", length = 100, nullable = false)
    private String titulo;
    @Column(name = "descricao", length = 100, nullable = false)
    private String descricao;
    @Column(name = "requisitos", length = 1000, nullable = false)
    private String requisitos;
    @Column(name = "beneficios", length = 1000, nullable = false)
    private String beneficios;
    @Column(name = "area", length = 100)
    private String area;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 9, nullable = false)
    private StatusVaga status;
    @Column(name = "data_abertura")
    private LocalDateTime dataAbertura;
    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    // Methods
    public void publicar(){
        if(this.status != StatusVaga.RASCUNHO)
            throw new IllegalStateException("Só pode publicar vaga em rascunho");
        this.status = StatusVaga.PUBLICADA;
    }

    public void encerrar(){
        if(this.status != StatusVaga.PUBLICADA){
            throw new IllegalStateException("Só pode encerrar vaga publicada");
        }
        this.status = StatusVaga.ENCERRADA;
    }

    public void arquivar(){
        if(this.status != StatusVaga.ENCERRADA){
            throw new IllegalStateException("Só pode arquivar vaga encerrada");
        }
        this.status = StatusVaga.ARQUIVADA;
    }

    public void rascunho(){
        if(this.status != StatusVaga.ARQUIVADA){
            throw new IllegalStateException("Só pode voltar para rascunho se estiver arquivada");
        }
        this.status = StatusVaga.RASCUNHO;
    }

    // Converters

    public ResponseVagaDTO toDTO(){
        return new ResponseVagaDTO(
                this.id,
                this.titulo,
                this.descricao,
                this.requisitos,
                this.beneficios,
                this.area,
                this.dataAbertura,
                this.dataEncerramento
        );
    }

    public void fromDTO(CreateVagaDTO dto){
        this.titulo = dto.titulo();
        this.descricao = dto.descricao();
        this.requisitos = dto.requisitos();
        this.beneficios = dto.beneficios();
        this.area = dto.area();
        this.dataAbertura = dto.dataAbertura();
        this.dataEncerramento = dto.dataEncerramento();
        this.status = StatusVaga.RASCUNHO;
    }

    public void fromDTO(UpdateVagaDTO dto){
        this.titulo = dto.titulo();
        this.descricao = dto.descricao();
        this.requisitos = dto.requisitos();
        this.beneficios = dto.beneficios();
        this.area = dto.area();
        this.dataEncerramento = dto.dataEncerramento();
    }
}
