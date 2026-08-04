package com.proautokimium.api.Application.DTOs.gallery;

import com.proautokimium.api.domain.enums.gallery.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public record GalleryDocumentResponseDTO(
        UUID id,
        String title,
        String description,
        Category category,
        String originalFilename,
        String contentType,
        LocalDateTime createdAt) {}
