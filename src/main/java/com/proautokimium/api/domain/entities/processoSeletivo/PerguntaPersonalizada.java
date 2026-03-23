package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private UUID vagaID;
    private String enunciado;
    private TipoPergunta tipo;
    private Boolean obrigatoria;
    private int ordem;
}
