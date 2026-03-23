package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private String titulo;
    private String descricao;
    private String requisitos;
    private String beneficios;
    private String area;
    private StatusVaga status;
    private LocalDateTime data_abertura;
    private LocalDateTime data_encerramento;
    private String criado_por;
}
