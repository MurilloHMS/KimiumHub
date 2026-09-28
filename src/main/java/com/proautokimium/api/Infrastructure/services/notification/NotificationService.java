package com.proautokimium.api.Infrastructure.services.notification;

import com.proautokimium.api.Application.DTOs.notification.NotificationDTO;
import com.proautokimium.api.Infrastructure.repositories.NotificationRepository;
import com.proautokimium.api.Infrastructure.services.push.WebPushService;
import com.proautokimium.api.domain.entities.Notification;
import com.proautokimium.api.domain.enums.NotificationType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        String safeTitle = fit(title, TITLE_MAX);
        String safeMessage = fit(message, MESSAGE_MAX);
        // Link cortado não abre nada: melhor sem link que com um quebrado.
        String safeLink = link != null && link.length() > LINK_MAX ? null : link;

        Notification saved = repository.save(new Notification(recipientLogin, type, safeTitle, safeMessage, safeLink));
        NotificationDTO dto = toDTO(saved);

        // A entrega ao vivo espera o commit. Dentro de um @Transactional o save
        // só grava no fim; entregar antes anunciava o que podia não existir —
        // em 2026-09-28 o celular recebeu o push de dois avisos que a
        // transação desfez, e nada apareceu na lista nem no mural.
        afterCommit(() -> deliverLive(recipientLogin, dto, safeTitle, safeMessage, safeLink));

        return dto;
    }

    /** Colunas de notifications (V49): título 200, mensagem 500, link 300. */
    static final int TITLE_MAX = 200;
    static final int MESSAGE_MAX = 500;
    static final int LINK_MAX = 300;

    /**
     * Corta no tamanho da coluna, com reticências. O aviso do mural aceita 4000
     * caracteres e manda o texto inteiro como mensagem: passar de 500 derrubava
     * a transação no commit. A notificação só precisa anunciar; o texto
     * completo continua no mural.
     */
    static String fit(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max - 1).stripTrailing() + "…";
    }

    /** Roda depois do commit; fora de transação, na hora. Em rollback, nunca. */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void deliverLive(String recipientLogin, NotificationDTO dto, String title, String message, String link) {
        // Empurrão em tempo real (STOMP) — destino por usuário (/user/{login}/queue/notifications)
        try {
            messagingTemplate.convertAndSendToUser(recipientLogin, "/queue/notifications", dto);
        } catch (Exception ignored) {
            // entrega ao vivo é best-effort; a notificação já está no banco
        }

        // Push nativo (mesmo com o app fechado)
        webPushService.sendToUser(recipientLogin, title, message, link);
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
