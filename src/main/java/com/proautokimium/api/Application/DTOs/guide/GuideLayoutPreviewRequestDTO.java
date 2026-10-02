package com.proautokimium.api.Application.DTOs.guide;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * A prévia do designer: o layout como está na tela — salvo ou não — com os
 * produtos que ele escolheu como exemplo.
 */
public record GuideLayoutPreviewRequestDTO(
        @NotNull(message = "O layout é obrigatório") JsonNode document,
        @NotEmpty(message = "Escolha ao menos um produto de exemplo")
        @Size(max = 30, message = "A prévia usa no máximo 30 produtos")
        List<UUID> productIds,
        String title
) {}
