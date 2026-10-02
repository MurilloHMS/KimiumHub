package com.proautokimium.api.Application.DTOs.guide;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/** O documento chega como objeto JSON, e não como texto dentro de texto. */
public record GuideLayoutDraftRequestDTO(@NotNull(message = "O layout é obrigatório") JsonNode document) {}
