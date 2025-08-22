package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("VENDEDOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendedor extends Funcionario {
    @Column(name = "departamento")
    private String departamento;
}
