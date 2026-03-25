package com.proautokimium.api.domain.entities.processoSeletivo;

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
    private LocalDateTime data_abertura;
    @Column(name = "data_encerramento")
    private LocalDateTime data_encerramento;

    // Methods
    public void publicar(){
        this.status = StatusVaga.PUBLICADA;
    }

    public void encerrar(){
        this.status = StatusVaga.ENCERRADA;
    }

    public void arquivar(){
        this.status = StatusVaga.ARQUIVADA;
    }

    public void rascunho(){
        this.status = StatusVaga.RASCUNHO;
    }
}
