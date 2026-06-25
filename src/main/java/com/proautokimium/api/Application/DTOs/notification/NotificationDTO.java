package com.proautokimium.api.Application.DTOs.notification;

import com.proautokimium.api.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt
) {
}
