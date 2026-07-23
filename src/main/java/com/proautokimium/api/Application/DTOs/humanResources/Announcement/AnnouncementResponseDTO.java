package com.proautokimium.api.Application.DTOs.humanResources.Announcement;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementResponseDTO(
        UUID id,
        String title,
        String content,
        UUID publishedById,
        String publishedByName,
        LocalDateTime publishedAt
) {
}
