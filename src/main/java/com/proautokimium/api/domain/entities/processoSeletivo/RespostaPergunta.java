package com.proautokimium.api.domain.entities.processoSeletivo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private UUID candidaturaID;
    private UUID perguntaID;
    private String resposta;
    private LocalDateTime respondido_em;
}
