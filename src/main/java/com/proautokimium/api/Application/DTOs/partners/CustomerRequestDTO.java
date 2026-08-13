package com.proautokimium.api.Application.DTOs.partners;

public record CustomerRequestDTO(String codParceiro,
		String documento,
		String nome,
		String email,
		String username,
		boolean ativo,
		boolean recebeEmail,
		String codMatriz,
	 	boolean isMatriz){

	/** CNPJ entra só com dígitos: a coluna tem 14 e o formatado tem 18. */
	public CustomerRequestDTO {
		documento = documento == null ? null : documento.replaceAll("[^0-9]", "");
	}
}
