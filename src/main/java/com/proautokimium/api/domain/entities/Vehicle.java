package com.proautokimium.api.domain.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    private static final long serialVersionUID= 1L;

    private String nome;
    private String placa;
    private String marca;
    private double consumoUrbanoAlcool;
    private double consumoUrbanoGasolina;
    private double consumoRodoviarioAlcool;
    private double consumoRodoviarioGasolina;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private Set<Revision> revisions = new HashSet<>();

    public Vehicle(String nome, String placa, String marca, double consumoUrbanoAlcool, double consumoUrbanoGasolina, double consumoRodoviarioAlcool, double consumoRodoviarioGasolina) {
        this.nome = nome;
        this.placa = placa;
        this.marca = marca;
        this.consumoUrbanoAlcool = consumoUrbanoAlcool;
        this.consumoUrbanoGasolina = consumoUrbanoGasolina;
        this.consumoRodoviarioAlcool = consumoRodoviarioAlcool;
        this.consumoRodoviarioGasolina = consumoRodoviarioGasolina;
    }
}
