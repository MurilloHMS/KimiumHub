package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.models.RedeSocial;
import com.proautokimium.api.domain.models.TelefoneContato;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(length = 150)
    private String cargo;

    @Column(length = 150)
    private String empresa;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 200, nullable = false))
    private Email email;

    @Column(length = 255)
    private String imagem;

    @Column(length = 255)
    private String banner;

    @Column(length = 255)
    private String site;

    @Column(length = 255)
    private String endereco;

    @Column(length = 1000)
    private String descricao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TelefoneContato> telefones = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<RedeSocial> redesSociais = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> regioesAtendimento = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> segmentosAtendimento = new ArrayList<>();

    @Column(nullable = false)
    private boolean ativo = true;
}