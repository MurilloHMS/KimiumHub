package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository.StatRow;
import com.proautokimium.api.Infrastructure.services.email.delivery.EmailDeliveryTrackingService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A análise da fila (mockup aprovado em 2026-10-07, blocos A a H): tendência,
 * funil, tempos, origem, provedor, endereços que falham sempre, horário e a
 * saúde do rastreio da Locaweb.
 *
 * <p>Uma consulta por período traz uma linha por e-mail, sem o corpo, e o resto
 * é contado aqui. Mediana e percentil não existem em JPQL, e o volume cabe: o
 * período para em um ano, e o ERP manda centenas a poucos milhares por mês.
 */
@Service
public class EmailQueueInsightsService {

    /** Até sair da fila: abaixo de 15 s, 15–30 s, 30 s–1 min, 1–2, 2–5, 5–15 min, 15 min ou mais. */
    static final List<Long> TO_SEND_EDGES = List.of(15L, 30L, 60L, 120L, 300L, 900L);
    /** Até chegar: abaixo de 1 s, 1–2, 2–5, 5–10, 10–30 s, 30 s–1 min, 1 min ou mais. */
    static final List<Long> TO_DELIVER_EDGES = List.of(1L, 2L, 5L, 10L, 30L, 60L);
    /** Os provedores com nome; o resto vira "outros". */
    static final int TOP_DOMAINS = 4;
    static final int MAX_PROBLEM_ADDRESSES = 10;

    private final EmailQueueRepository repository;
    private final EmailQueueAdminService admin;
    private final EmailDeliveryTrackingService tracking;
    private final Clock clock;

    public EmailQueueInsightsService(EmailQueueRepository repository, EmailQueueAdminService admin,
                                     EmailDeliveryTrackingService tracking, Clock clock) {
        this.repository = repository;
        this.admin = admin;
        this.tracking = tracking;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Insights insights(Integer daysParam, LocalDate sinceDate) {
        int days = admin.days(daysParam, sinceDate);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = admin.since(days);
        // O período anterior tem o mesmo tamanho e termina onde este começa.
        List<StatRow> rows = repository.statRows(from, now.plusYears(100));
        List<StatRow> before = repository.statRows(from.minusDays(days), from);

        return new Insights(totals(rows), totals(before), funnel(rows),
                timing(rows, r -> r.getStatus() == EmailStatus.SENT ? seconds(r.getCreatedAt(), r.getSentAt()) : null, TO_SEND_EDGES),
                timing(rows, r -> seconds(r.getSentAt(), r.getDeliveredAt()), TO_DELIVER_EDGES),
                origins(rows), domains(rows), problemAddresses(rows), perHour(rows), health(rows, now));
    }

    // ── A ──

    static Totals totals(List<StatRow> rows) {
        long failed = count(rows, r -> r.getStatus() == EmailStatus.FAILED);
        long sent = count(rows, r -> r.getStatus() == EmailStatus.SENT);
        long retried = count(rows, r -> r.getStatus() == EmailStatus.SENT && attempts(r) > 1);
        long bounced = count(rows, r -> r.getBouncedAt() != null);
        return new Totals(failed, sent, retried, bounced, deliveryRate(rows));
    }

    /** Entregues ÷ rastreados concluídos (enviados + falharam): a mesma conta do resumo. */
    static Double deliveryRate(List<StatRow> rows) {
        List<StatRow> done = rows.stream().filter(r -> r.getTrackingId() != null
                && (r.getStatus() == EmailStatus.SENT || r.getStatus() == EmailStatus.FAILED)).toList();
        if (done.isEmpty()) return null;
        return round1(count(done, r -> r.getDeliveredAt() != null) * 100.0 / done.size());
    }

    // ── B ──

    static Funnel funnel(List<StatRow> rows) {
        List<StatRow> tracked = rows.stream().filter(r -> r.getTrackingId() != null).toList();
        return new Funnel(tracked.size(),
                count(tracked, r -> r.getStatus() == EmailStatus.SENT),
                count(tracked, r -> r.getDeliveredAt() != null),
                count(tracked, r -> r.getStatus() == EmailStatus.PENDING || r.getStatus() == EmailStatus.SCHEDULED),
                count(tracked, r -> r.getStatus() == EmailStatus.FAILED),
                count(tracked, r -> r.getBouncedAt() != null),
                count(tracked, r -> r.getStatus() == EmailStatus.SENT && r.getDeliveredAt() == null && r.getBouncedAt() == null));
    }

    // ── C ──

    static Timing timing(List<StatRow> rows, java.util.function.Function<StatRow, Long> measure, List<Long> edges) {
        List<Long> values = rows.stream().map(measure).filter(Objects::nonNull).sorted().toList();
        long[] buckets = new long[edges.size() + 1];
        for (long v : values) {
            int i = 0;
            while (i < edges.size() && v >= edges.get(i)) i++;
            buckets[i]++;
        }
        return new Timing(values.size(), percentile(values, 50), percentile(values, 95), edges,
                Arrays.stream(buckets).boxed().toList());
    }

    /** Posição mais próxima (nearest-rank): o valor que de fato aconteceu, sem interpolar. */
    static Long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return null;
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    /** Segundos entre os dois, ou nulo; negativo (relógio torto) vira zero. */
    static Long seconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return null;
        return Math.max(0, Duration.between(start, end).toSeconds());
    }

    // ── D ──

    static List<OriginInsight> origins(List<StatRow> rows) {
        Map<Optional<EmailOrigin>, List<StatRow>> byOrigin = rows.stream()
                .collect(Collectors.groupingBy(r -> Optional.ofNullable(r.getOrigin()), LinkedHashMap::new, Collectors.toList()));
        return byOrigin.entrySet().stream()
                .map(e -> {
                    List<StatRow> list = e.getValue();
                    EmailOrigin origin = e.getKey().orElse(null);
                    return new OriginInsight(origin, EmailQueueAdminService.label(origin), list.size(),
                            count(list, r -> r.getStatus() == EmailStatus.FAILED), deliveryRate(list),
                            timing(list, r -> r.getStatus() == EmailStatus.SENT ? seconds(r.getCreatedAt(), r.getSentAt()) : null,
                                    TO_SEND_EDGES).medianSeconds());
                })
                .sorted(Comparator.comparingLong(OriginInsight::total).reversed())
                .toList();
    }

    // ── E ──

    static List<DomainInsight> domains(List<StatRow> rows) {
        Map<String, List<StatRow>> byDomain = rows.stream().filter(r -> domainOf(r.getToEmail()) != null)
                .collect(Collectors.groupingBy(r -> domainOf(r.getToEmail())));
        List<Map.Entry<String, List<StatRow>>> sorted = byDomain.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<StatRow>>>comparingInt(e -> e.getValue().size()).reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();
        List<DomainInsight> out = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_DOMAINS, sorted.size()); i++) out.add(domain(sorted.get(i).getKey(), sorted.get(i).getValue()));
        if (sorted.size() > TOP_DOMAINS) {
            List<StatRow> rest = sorted.subList(TOP_DOMAINS, sorted.size()).stream().flatMap(e -> e.getValue().stream()).toList();
            out.add(domain("outros (" + (sorted.size() - TOP_DOMAINS) + ")", rest));
        }
        return out;
    }

    private static DomainInsight domain(String name, List<StatRow> list) {
        return new DomainInsight(name, list.size(), deliveryRate(list), count(list, r -> r.getBouncedAt() != null));
    }

    static String domainOf(String address) {
        if (address == null) return null;
        int at = address.lastIndexOf('@');
        return at < 0 || at == address.length() - 1 ? null : address.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }

    // ── F ──

    static List<ProblemAddress> problemAddresses(List<StatRow> rows) {
        Map<String, List<StatRow>> problems = rows.stream()
                .filter(r -> r.getToEmail() != null && (r.getStatus() == EmailStatus.FAILED || r.getBouncedAt() != null))
                .collect(Collectors.groupingBy(r -> r.getToEmail().trim().toLowerCase(Locale.ROOT)));
        return problems.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .map(e -> {
                    StatRow last = e.getValue().stream().max(Comparator.comparing(StatRow::getCreatedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder()))).orElseThrow();
                    EmailFailureKind kind = Optional.ofNullable(EmailFailureKind.classify(
                            last.getBouncedAt() != null ? last.getBounceReason() : last.getLastError())).orElse(EmailFailureKind.OTHER);
                    return new ProblemAddress(e.getKey(), e.getValue().size(), kind,
                            last.getBouncedAt() != null && kind == EmailFailureKind.OTHER ? "Devolvido" : kind.getLabel(),
                            last.getOrigin(), EmailQueueAdminService.label(last.getOrigin()));
                })
                .sorted(Comparator.comparingLong(ProblemAddress::times).reversed().thenComparing(ProblemAddress::address))
                .limit(MAX_PROBLEM_ADDRESSES)
                .toList();
    }

    // ── H ──

    static List<Long> perHour(List<StatRow> rows) {
        long[] hours = new long[24];
        rows.stream().filter(r -> r.getStatus() == EmailStatus.SENT && r.getSentAt() != null)
                .forEach(r -> hours[r.getSentAt().getHour()]++);
        return Arrays.stream(hours).boxed().toList();
    }

    // ── G ──

    private TrackingHealth health(List<StatRow> rows, LocalDateTime now) {
        LocalDateTime windowStart = now.minus(EmailQueue.DELIVERY_WINDOW);
        Predicate<StatRow> waiting = r -> r.getStatus() == EmailStatus.SENT && r.getTrackingId() != null
                && r.getDeliveredAt() == null && r.getBouncedAt() == null && r.getSentAt() != null;
        long awaiting = count(rows, waiting.and(r -> !r.getSentAt().isBefore(windowStart)));
        long unconfirmed = count(rows, waiting.and(r -> r.getSentAt().isBefore(windowStart)));
        Optional<EmailDeliveryTrackingService.LastRun> last = tracking.lastRun();
        return new TrackingHealth(tracking.isEnabled(), awaiting, unconfirmed,
                last.map(EmailDeliveryTrackingService.LastRun::at).orElse(null),
                last.map(EmailDeliveryTrackingService.LastRun::ok).orElse(null),
                last.map(EmailDeliveryTrackingService.LastRun::pages).orElse(null),
                last.map(EmailDeliveryTrackingService.LastRun::error).orElse(null));
    }

    // ── comum ──

    private static long count(List<StatRow> rows, Predicate<StatRow> p) {
        return rows.stream().filter(p).count();
    }

    private static int attempts(StatRow r) {
        return r.getAttempts() == null ? 0 : r.getAttempts();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
