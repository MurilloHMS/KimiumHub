package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
}
