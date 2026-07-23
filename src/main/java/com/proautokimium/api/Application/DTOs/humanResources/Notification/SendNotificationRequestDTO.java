package com.proautokimium.api.Application.DTOs.humanResources.Notification;

import java.util.List;
import java.util.UUID;

/** employeeIds nulo ou vazio = envia pra todos os funcionários ativos. */
public record SendNotificationRequestDTO(
        List<UUID> employeeIds,
        String title,
        String message,
        String link
) {
}
