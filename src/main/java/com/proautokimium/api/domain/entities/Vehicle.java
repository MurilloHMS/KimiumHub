package com.proautokimium.api.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends com.proautokimium.api.domain.abstractions.Entity {

    private String nome;
    private String placa;
    private String marca;
    private double consumoUrbanoAlcool;
    private double consumoUrbanoGasolina;
    private double consumoRodoviarioAlcool;
    private double consumoRodoviarioGasolina;

}
