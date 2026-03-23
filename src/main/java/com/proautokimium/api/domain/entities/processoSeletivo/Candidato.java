package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidatos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Candidato extends com.proautokimium.api.domain.abstractions.Entity{
    private String nome;
    private Email email;
    private String telefone;
    private String urlLinkedin;
    private String pathCurriculo;
    private LocalDateTime criado_em;
}
