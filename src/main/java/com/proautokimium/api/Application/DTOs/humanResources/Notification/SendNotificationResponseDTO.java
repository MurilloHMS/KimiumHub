package com.proautokimium.api.Application.DTOs.humanResources.Notification;

public record SendNotificationResponseDTO(
        int notified,
        int skippedNoAccount
) {
}
