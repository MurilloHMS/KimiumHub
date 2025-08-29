package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.Hierarchy;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("FUNCIONARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Employee extends Partner {
    @Column(name = "codigo_gerente", length = 9)
    private String codigoGerente;
    @Column(name = "hierarquia", length = 15)
    private Hierarchy hierarquia;
}
