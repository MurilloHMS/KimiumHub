package com.proautokimium.api.Application.DTOs.humanResources.Announcement;

import java.util.UUID;

public record CreateAnnouncementRequestDTO(
        UUID publishedByEmployeeId,
        String title,
        String content
) {
}
