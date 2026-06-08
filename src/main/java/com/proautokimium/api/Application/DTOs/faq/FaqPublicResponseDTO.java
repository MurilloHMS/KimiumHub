package com.proautokimium.api.Application.DTOs.faq;

import com.proautokimium.api.domain.enums.StatusPostagem;

public record FaqPublicResponseDTO(String title, String body, StatusPostagem status) {
}
