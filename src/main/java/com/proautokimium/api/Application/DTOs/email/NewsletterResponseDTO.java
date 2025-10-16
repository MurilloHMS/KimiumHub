package com.proautokimium.api.Application.DTOs.email;

import java.time.LocalDate;

import com.proautokimium.api.domain.enums.EmailStatus;

public record NewsletterResponseDTO(String codigoCliente,
		String nomeCliente,
		LocalDate data,
		String mes,
		int quantidadeProdutos,
		double quantidadeLitros,
		int quantidadeNotasEmitidas,
		int mediaDiasAtendimento,
		String produtoDestaque,
		double faturamentoTotal,
		double valorPecasTrocadas,
		double valorTotalDeHoras,
		Double valorTotalCobradoHoras,
		EmailStatus status,
		String email) {

}
