package com.proautokimium.api.Infrastructure.services.notification;

import com.proautokimium.api.Application.DTOs.notification.NotificationDTO;
import com.proautokimium.api.Infrastructure.repositories.NotificationRepository;
import com.proautokimium.api.Infrastructure.services.push.WebPushService;
import com.proautokimium.api.domain.entities.Notification;
import com.proautokimium.api.domain.enums.NotificationType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;

    public NotificationService(NotificationRepository repository,
                               SimpMessagingTemplate messagingTemplate,
                               WebPushService webPushService) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.webPushService = webPushService;
    }

    /**
     * Cria a notificação, persiste e entrega ao vivo:
     * - STOMP para a fila do usuário (se ele estiver com o site aberto);
     * - Web Push para os dispositivos inscritos (mesmo com o site fechado).
     */
    @Transactional
    public NotificationDTO notify(String recipientLogin, NotificationType type,
                                  String title, String message, String link) {
        Notification saved = repository.save(new Notification(recipientLogin, type, title, message, link));
        NotificationDTO dto = toDTO(saved);

        // Empurrão em tempo real (STOMP) — destino por usuário (/user/{login}/queue/notifications)
        try {
            messagingTemplate.convertAndSendToUser(recipientLogin, "/queue/notifications", dto);
        } catch (Exception ignored) {
            // entrega ao vivo é best-effort; a notificação já está persistida
        }

        // Push nativo (mesmo com o app fechado)
        webPushService.sendToUser(recipientLogin, title, message, link);

        return dto;
    }

    public List<NotificationDTO> listar(String login) {
        return repository.findByRecipientLoginOrderByCreatedAtDesc(login).stream()
                .map(this::toDTO)
                .toList();
    }

    public long contarNaoLidas(String login) {
        return repository.countByRecipientLoginAndReadFalse(login);
    }

    @Transactional
    public boolean marcarComoLida(UUID id, String login) {
        Notification n = repository.findById(id).orElse(null);
        if (n == null || !n.getRecipientLogin().equals(login)) return false;
        if (!n.isRead()) {
            n.setRead(true);
            repository.save(n);
        }
        return true;
    }

    @Transactional
    public int marcarTodasComoLidas(String login) {
        return repository.markAllReadByRecipient(login);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
