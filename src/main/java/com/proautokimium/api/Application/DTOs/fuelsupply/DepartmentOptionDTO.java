package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.util.UUID;

/**
 * Um departamento como opcao da lista da conferencia: id e nome, nada mais.
 *
 * <p>Nao reaproveita o {@code DepartmentResponseDTO} do RH porque a tela de
 * abastecimento nao precisa do resto e, principalmente, porque o
 * {@code GET /api/hr/departments} exige autoridade de RH: quem tem
 * {@code company/fuel-supply:INCLUIR} e nao trabalha no RH abriria a
 * conferencia com o combo vazio e um 403 escondido no console.
 */
public record DepartmentOptionDTO(UUID id, String name) { }
