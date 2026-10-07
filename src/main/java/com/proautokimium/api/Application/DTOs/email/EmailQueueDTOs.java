package com.proautokimium.api.Application.DTOs.email;

import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailDeliveryState;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Os formatos da tela do desenvolvedor: fila de e-mails e remetentes. */
public final class EmailQueueDTOs {

    private EmailQueueDTOs() {}

    public record EmailRow(UUID id, String to, String subject, EmailOrigin origin, String originLabel,
                           EmailStatus status, int attempts, LocalDateTime createdAt, LocalDateTime sentAt,
                           LocalDateTime lastAttemptAt, String lastError, EmailFailureKind failureKind,
                           String failureLabel, boolean hasAttachments, boolean resendable,
                           EmailDeliveryState deliveryState, LocalDateTime deliveredAt, LocalDateTime bouncedAt) {}

    public record EmailPage(long total, List<EmailRow> items) {}

    public record AttachmentInfo(String filename, String contentType, long sizeBytes) {}

    public record EmailDetail(UUID id, String to, String subject, EmailOrigin origin, String originLabel,
                              EmailStatus status, int attempts, LocalDateTime createdAt, LocalDateTime sentAt,
                              LocalDateTime lastAttemptAt, String lastError, EmailFailureKind failureKind,
                              String failureLabel, boolean hasAttachments, boolean resendable,
                              EmailDeliveryState deliveryState, LocalDateTime deliveredAt, LocalDateTime bouncedAt,
                              String bounceReason, String from, String fromName,
                              String replyTo, String body, boolean bodyHidden, List<AttachmentInfo> attachments) {}

    public record DayStat(LocalDate date, long sent, long failed, long retried) {}

    public record ReasonStat(EmailFailureKind kind, String label, long count, String sample) {}

    public record OriginStat(EmailOrigin origin, String label, long total, long failed) {}

    public record Summary(int days, long failed, long queued, LocalDateTime oldestQueuedAt, long sent,
                          Double successRate, long retried, double avgAttempts, LocalDateTime lastActivityAt,
                          List<DayStat> perDay, List<ReasonStat> reasons, List<OriginStat> origins,
                          Delivery delivery) {}

    /**
     * A entrega no período, só dos e-mails rastreados (os de antes da V122 não
     * têm como). {@code rate} = entregues ÷ (enviados + falharam), como no
     * mockup aprovado; nulo quando não há nenhum rastreado concluído.
     */
    public record Delivery(long tracked, long delivered, long bounced, long awaiting, Double rate) {}

    public record ResendRequest(List<UUID> ids) {}

    public record ResendResult(int requeued) {}

    // ── Remetentes ──

    public record Sender(UUID id, String name, String address, String displayName, boolean active,
                         boolean isDefault, Set<EmailOrigin> usedBy) {}

    public record CreateSender(String name, String displayName) {}

    public record UpdateSender(String displayName, Boolean active) {}

    public record Route(EmailOrigin origin, String label, String hint, UUID senderId, UUID replyToId, long last30Days) {}

    public record UpdateRoute(UUID senderId, UUID replyToId) {}
}
