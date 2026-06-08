package com.proautokimium.api.Application.DTOs.guide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * DTO de requisição para geração do Guia de Utilização.
 *
 * @param tituloGuia  Título exibido no cabeçalho (ex: "GERAL", "AÇOUGUE")
 * @param productIds  IDs dos produtos que devem aparecer no guia, na ordem desejada
 */
public record GuideReportRequestDTO(

        @NotBlank(message = "O título do guia é obrigatório")
        String tituloGuia,

        @NotEmpty(message = "Selecione ao menos um produto")
        List<UUID> productIds
) {}