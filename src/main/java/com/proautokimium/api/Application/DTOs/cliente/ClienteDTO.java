package com.proautokimium.api.Application.DTOs.cliente;

public record ClienteDTO (String codParceiro, String documento, String nome, String email, boolean ativo, boolean recebeEmail){}
