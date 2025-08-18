package com.proautokimium.api.Application.DTOs.vehicle;

public record VehicleRequestDTO(String nome, String placa, String marca, double consumoUrbanoAlcool, double consumoUrbanoGasolina, double consumoRodoviarioAlcool, double consumoRodoviarioGasolina){}
