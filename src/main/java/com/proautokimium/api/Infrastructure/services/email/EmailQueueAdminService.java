package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.exceptions.EmailRecordNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * A tela do desenvolvedor sobre a fila: a lista com filtros, os indicadores e
 * o reenvio. As contagens vêm de consultas agrupadas, sem carregar o corpo dos
 * e-mails (que pode ter dezenas de KB cada).
 */
@Service
public class EmailQueueAdminService {

    static final List<EmailStatus> QUEUED = List.of(EmailStatus.PENDING, EmailStatus.SCHEDULED);
    static final Set<Integer> ALLOWED_DAYS = Set.of(1, 7, 30);
    static final int MAX_PAGE_SIZE = 200;

    private final EmailQueueRepository repository;
    private final Clock clock;

    public EmailQueueAdminService(EmailQueueRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** O começo do período: "hoje" é desde a meia-noite; 7 e 30 dias contam o dia de hoje. */
    LocalDateTime since(int days) {
        return LocalDate.now(clock).minusDays(days - 1L).atStartOfDay();
    }

    static int days(Integer days) {
        return days != null && ALLOWED_DAYS.contains(days) ? days : 7;
    }

    @Transactional(readOnly = true)
    public EmailPage list(String status, EmailOrigin origin, Integer days, String q, int page, int size) {
        LocalDateTime since = since(days(days));
        boolean failedOnly = "FAILED".equals(status);
        Specification<EmailQueue> spec = (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            // Na fila aparece sempre, de qualquer data: é o que ainda vai sair.
            and.add(cb.or(cb.greaterThanOrEqualTo(root.get("createdAt"), since), root.get("status").in(QUEUED)));
            if ("QUEUE".equals(status)) and.add(root.get("status").in(QUEUED));
            else if (status != null && !status.isBlank()) and.add(cb.equal(root.get("status"), EmailStatus.valueOf(status)));
            if (origin != null) and.add(cb.equal(root.get("origin"), origin));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                and.add(cb.or(cb.like(cb.lower(root.get("toEmail")), like), cb.like(cb.lower(root.get("subject")), like)));
            }
            return cb.and(and.toArray(Predicate[]::new));
        };
        // Os que falharam, do mais antigo (o esquecido) para o mais novo; o resto, do mais novo.
        Sort sort = failedOnly ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Page<EmailQueue> result = repository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort));
        return new EmailPage(result.getTotalElements(), result.getContent().stream().map(EmailQueueAdminService::row).toList());
    }

    @Transactional(readOnly = true)
    public Summary summary(Integer daysParam) {
        int days = days(daysParam);
        LocalDateTime since = since(days);

        Map<EmailStatus, Long> byStatus = new EnumMap<>(EmailStatus.class);
        repository.countByStatusSince(since).forEach(c -> byStatus.merge(c.getStatus(), c.getTotal(), Long::sum));
        long sent = byStatus.getOrDefault(EmailStatus.SENT, 0L);
        long failed = byStatus.getOrDefault(EmailStatus.FAILED, 0L);
        long done = sent + failed;
        double successRate = done == 0 ? 100.0 : Math.round(sent * 1000.0 / done) / 10.0;

        // Um dia por dia do período, com os zerados: o gráfico não pode pular dia.
        Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (int i = days - 1; i >= 0; i--) perDay.put(LocalDate.now(clock).minusDays(i), new long[3]);
        for (EmailQueueRepository.DayCount c : repository.countByDaySince(since)) {
            long[] d = perDay.get(c.getDay());
            if (d == null) continue;
            if (c.getStatus() == EmailStatus.SENT) { d[0] += c.getTotal(); d[2] += c.getRetried(); }
            if (c.getStatus() == EmailStatus.FAILED) d[1] += c.getTotal();
        }

        Map<EmailFailureKind, long[]> reasonCount = new EnumMap<>(EmailFailureKind.class);
        Map<EmailFailureKind, String> samples = new EnumMap<>(EmailFailureKind.class);
        for (String error : repository.errorsSince(EmailStatus.FAILED, since)) {
            EmailFailureKind kind = Optional.ofNullable(EmailFailureKind.classify(error)).orElse(EmailFailureKind.OTHER);
            reasonCount.computeIfAbsent(kind, k -> new long[1])[0]++;
            samples.putIfAbsent(kind, error);
        }
        List<ReasonStat> reasons = reasonCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> new ReasonStat(e.getKey(), e.getKey().getLabel(), e.getValue()[0], samples.get(e.getKey())))
                .toList();

        Map<EmailOrigin, long[]> byOrigin = new LinkedHashMap<>();
        for (EmailQueueRepository.OriginCount c : repository.countByOriginSince(since)) {
            long[] o = byOrigin.computeIfAbsent(c.getOrigin(), k -> new long[2]);
            o[0] += c.getTotal();
            if (c.getStatus() == EmailStatus.FAILED) o[1] += c.getTotal();
        }
        List<OriginStat> origins = byOrigin.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> new OriginStat(e.getKey(), label(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new Summary(days, failed, repository.countByStatusIn(QUEUED), repository.oldestCreatedAt(QUEUED), sent,
                successRate, repository.countRetriedSince(EmailStatus.SENT, since),
                Math.round(repository.averageAttemptsSince(List.of(EmailStatus.SENT, EmailStatus.FAILED), since) * 100) / 100.0,
                repository.lastActivityAt(),
                perDay.entrySet().stream().map(e -> new DayStat(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList(),
                reasons, origins);
    }

    @Transactional(readOnly = true)
    public EmailDetail detail(UUID id) {
        EmailQueue e = find(id);
        boolean hidden = e.getOrigin() != null && e.getOrigin().isSensitive();
        EmailFailureKind kind = EmailFailureKind.classify(e.getLastError());
        return new EmailDetail(e.getId(), e.getToEmail(), e.getSubject(), e.getOrigin(), label(e.getOrigin()),
                e.getStatus(), e.getAttempts(), e.getCreatedAt(), e.getSentAt(), e.getLastAttemptAt(), e.getLastError(),
                kind, kind == null ? null : kind.getLabel(), !e.getAttachments().isEmpty(), e.getFromEmail(),
                e.getFromName(), e.getReplyTo(), hidden ? null : e.getBody(), hidden,
                e.getAttachments().stream().map(a -> new AttachmentInfo(a.getFilename(), a.getContentType(), a.getSizeBytes())).toList());
    }

    /** Só o que falhou volta para a fila; a regra mora na entidade (requeue). */
    @Transactional
    public EmailRow resend(UUID id) {
        EmailQueue e = find(id);
        e.requeue();
        return row(repository.save(e));
    }

    /** Em lote: os que não falharam são ignorados, para um clique não reenviar o que já saiu. */
    @Transactional
    public ResendResult resend(List<UUID> ids) {
        int requeued = 0;
        for (EmailQueue e : repository.findAllById(ids == null ? List.of() : ids)) {
            if (e.getStatus() != EmailStatus.FAILED) continue;
            e.requeue();
            repository.save(e);
            requeued++;
        }
        return new ResendResult(requeued);
    }

    private EmailQueue find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EmailRecordNotFoundException("E-mail não encontrado na fila."));
    }

    static String label(EmailOrigin origin) {
        return origin == null ? "Sem origem" : origin.getLabel();
    }

    static EmailRow row(EmailQueue e) {
        EmailFailureKind kind = EmailFailureKind.classify(e.getLastError());
        return new EmailRow(e.getId(), e.getToEmail(), e.getSubject(), e.getOrigin(), label(e.getOrigin()), e.getStatus(),
                e.getAttempts(), e.getCreatedAt(), e.getSentAt(), e.getLastAttemptAt(), e.getLastError(), kind,
                kind == null ? null : kind.getLabel(), !e.getAttachments().isEmpty());
    }
}
