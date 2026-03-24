package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
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
    @Column(name = "nome", length = 100, nullable = false)
    private String nome;
    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 100, nullable = false))
    private Email email;
    @Column(name = "telefone", length = 11,  nullable = false)
    private String telefone;
    @Column(name = "url_linkedin", length = 100)
    private String urlLinkedin;
    @Column(name = "path_curriculo", length = 200)
    private String pathCurriculo;
    @Column(name = "criado_em" )
    private LocalDateTime criado_em;
}
