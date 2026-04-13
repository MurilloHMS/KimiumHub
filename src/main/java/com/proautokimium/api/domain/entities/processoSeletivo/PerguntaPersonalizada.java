package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.ResponsePerguntaPersonalizadaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perguntas_personalizadas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PerguntaPersonalizada extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;
    private String enunciado;
    @Enumerated(EnumType.STRING)
    private TipoPergunta tipo;
    private Boolean obrigatoria;
    private short ordem;

    // Methods

    public void fromDTO(CreatePerguntaDTO dto){
        this.enunciado = dto.enunciado();
        this.tipo = dto.tipo();
        this.obrigatoria = dto.obrigatoria();
        this.ordem = dto.ordem();
    }

    public void fromDTO(UpdatePerguntaDTO dto){
        this.enunciado = dto.enunciado();
        this.tipo = dto.tipo();
        this.obrigatoria = dto.obrigatoria();
        this.ordem = dto.ordem();
    }

    public ResponsePerguntaPersonalizadaDTO toDTO(){
        return new ResponsePerguntaPersonalizadaDTO(
                this.id,
                this.enunciado,
                this.tipo,
                this.obrigatoria,
                this.ordem
        );
    }
}
