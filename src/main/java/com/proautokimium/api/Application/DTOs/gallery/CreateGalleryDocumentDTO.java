package com.proautokimium.api.Application.DTOs.gallery;

import com.proautokimium.api.domain.enums.gallery.Category;

public record CreateGalleryDocumentDTO(
        String title,
        String description,
        Category category) { }
