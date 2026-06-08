package com.proautokimium.api.Application.DTOs.faq;

import com.proautokimium.api.domain.enums.StatusPostagem;

import java.util.UUID;

public record FaqResponseDTO(UUID id, String title, String body, StatusPostagem status) {
}
