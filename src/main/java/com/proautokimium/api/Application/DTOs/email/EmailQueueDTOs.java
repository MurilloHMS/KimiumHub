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

    // ── Indicadores da análise (mockup aprovado em 2026-10-07, blocos A a H) ──

    /**
     * Tudo o que a tela calcula sobre o período, numa resposta só. Contado em
     * Java sobre uma projeção sem o corpo dos e-mails: mediana e percentil não
     * existem em JPQL, e o volume (centenas a poucos milhares por mês) cabe.
     */
    public record Insights(Totals current, Totals previous, Funnel funnel, Timing toSend, Timing toDeliver,
                           List<OriginInsight> origins, List<DomainInsight> domains,
                           List<ProblemAddress> problemAddresses, List<Long> perHour, TrackingHealth tracking) {}

    /** A: os números de cima, para comparar com o período de mesmo tamanho logo antes. */
    public record Totals(long failed, long sent, long retried, long bounced, Double deliveryRate) {}

    /** B: só os rastreados (de depois da V122), do pedido à caixa de entrada. */
    public record Funnel(long created, long sent, long delivered, long queued, long failed, long bounced, long unconfirmed) {}

    /**
     * C: em segundos. {@code buckets} tem um a mais que {@code edges}: o
     * primeiro conta abaixo de edges[0], o último de edges[n-1] para cima.
     */
    public record Timing(long count, Long medianSeconds, Long p95Seconds, List<Long> edges, List<Long> buckets) {}

    /** D: o "Por origem", agora com a entrega e o tempo até sair. */
    public record OriginInsight(EmailOrigin origin, String label, long total, long failed, Double deliveryRate,
                                Long medianToSendSeconds) {}

    /** E: o provedor de quem recebe (a parte depois do @). O último item pode ser "outros". */
    public record DomainInsight(String domain, long total, Double deliveryRate, long bounced) {}

    /** F: endereço com duas ou mais falhas ou devoluções: o conserto é no cadastro. */
    public record ProblemAddress(String address, long times, EmailFailureKind lastKind, String lastLabel,
                                 EmailOrigin origin, String originLabel) {}

    /** G: o rastreio da Locaweb está vivo? A última passada fica em memória (zera quando a API sobe). */
    public record TrackingHealth(boolean enabled, long awaiting, long unconfirmed, LocalDateTime lastRunAt,
                                 Boolean lastRunOk, Integer lastRunPages, String lastRunError) {}

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
