package com.proautokimium.api.Application.DTOs.partners;

public record ServiceLocationDTO(String codParceiro,
                                 String documento,
                                 String nome,
                                 String email,
                                 boolean ativo,
                                 String address) {}
