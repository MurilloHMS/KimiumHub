package com.proautokimium.api.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends com.proautokimium.api.domain.abstractions.Entity {

    public String nome;
    public String placa;
    public String marca;
    public double consumoUrbanoAlcool;
    public double consumoUrbanoGasolina;
    public double consumoRodoviarioAlcool;
    public double consumoRodoviarioGasolina;

}
