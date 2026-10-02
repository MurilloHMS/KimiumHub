package com.proautokimium.api.Application.DTOs.guide;

import jakarta.validation.constraints.Size;

public record GuideLayoutPublishRequestDTO(
        @Size(max = 300, message = "A nota tem no máximo 300 caracteres") String note
) {}
