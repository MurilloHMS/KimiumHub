package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "perfil", discriminatorType = DiscriminatorType.STRING)
@Table(name = "parceiros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Parceiro extends com.proautokimium.api.domain.abstractions.Entity {
    @Column(name = "cod_parceiro", length = 9, nullable = false)
    private String codParceiro;
    @Column(name = "documento", length = 14)
    private String documento;
    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 200, nullable = false))
    private Email email;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
