package com.proautokimium.api.domain.entities.processoSeletivo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resposta_perguntas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RespostaPergunta extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaPersonalizada pergunta;

    private String resposta;
    private LocalDateTime respondidoEm;
}
