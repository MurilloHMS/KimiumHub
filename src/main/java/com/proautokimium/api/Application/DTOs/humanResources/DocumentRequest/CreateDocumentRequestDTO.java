package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

/** O rascunho nasce só com o título; o resto vem na edição. */
public record CreateDocumentRequestDTO(String title) {
}
