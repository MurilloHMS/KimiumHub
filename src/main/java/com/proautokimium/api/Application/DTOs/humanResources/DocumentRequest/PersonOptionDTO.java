package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.util.UUID;

/**
 * Uma pessoa no seletor de público das Solicitações. Os mesmos campos da opção
 * dos Eventos ({@code id}, {@code name}, {@code detail}), mais {@code hasAccess}:
 * quem não tem login também recebe, e o RH registra a resposta dele.
 */
public record PersonOptionDTO(UUID id, String name, String detail, boolean hasAccess) {
}
