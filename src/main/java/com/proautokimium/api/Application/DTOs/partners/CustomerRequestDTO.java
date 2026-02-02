package com.proautokimium.api.Application.DTOs.partners;

public record CustomerRequestDTO(String codParceiro,
		String documento,
		String nome,
		String email,
		String username,
		boolean ativo,
		boolean recebeEmail,
		String codMatriz){}
