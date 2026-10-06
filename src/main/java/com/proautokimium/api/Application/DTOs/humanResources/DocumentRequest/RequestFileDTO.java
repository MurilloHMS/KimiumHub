package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/** O arquivo atual de um campo. O conteúdo vem pelo download, nunca aqui. */
public record RequestFileDTO(UUID id, String fieldKey, String originalFilename, LocalDateTime uploadedAt) {
}
