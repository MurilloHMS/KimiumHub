package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.Hierarquia;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("funcionarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Funcionario extends Parceiro{
    @Column(name = "codigo_gerente", length = 9)
    private String codigoGerente;
    @Column(name = "hierarquia", length = 15)
    private Hierarquia hierarquia;
}
