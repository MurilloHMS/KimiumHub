package com.proautokimium.api.Infrastructure.services.email.delivery;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.delivery.LocawebReportClient.ReportPage;
import com.proautokimium.api.Infrastructure.services.email.delivery.LocawebReportClient.ReportedMessage;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casa os e-mails enviados com o relatório do SMTP Locaweb e grava a entrega.
 *
 * <p>Pergunta só pelos enviados que ainda esperam confirmação, dentro da
 * {@link EmailQueue#DELIVERY_WINDOW}; sem nenhum, nem chama a API. A Locaweb não
 * filtra pelo nosso cabeçalho, então a lista do período é paginada até achar
 * todos os esperados (ou acabar).
 *
 * <p>Sem transação em volta: as chamadas HTTP levam segundos e não seguram
 * conexão com o banco. Cada marca é um UPDATE próprio, que só vale se a linha
 * ainda não foi marcada.
 */
@Service
public class EmailDeliveryTrackingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeliveryTrackingService.class);

    /** Teto de páginas por passada: 50 por página, 2.000 mensagens no período. */
    static final int MAX_PAGES = 40;

    private final EmailQueueRepository repository;
    private final LocawebReportClient client;
    private final Clock clock;

    public EmailDeliveryTrackingService(EmailQueueRepository repository, LocawebReportClient client, Clock clock) {
        this.repository = repository;
        this.client = client;
        this.clock = clock;
    }

    public record Result(int awaiting, int delivered, int bounced, int pages) {}

    public Result track() {
        if (!client.isEnabled()) return new Result(0, 0, 0, 0);

        LocalDateTime now = LocalDateTime.now(clock);
        List<EmailQueueRepository.AwaitingDelivery> awaiting =
                repository.findAwaitingDelivery(EmailStatus.SENT, now.minus(EmailQueue.DELIVERY_WINDOW));
        if (awaiting.isEmpty()) return new Result(0, 0, 0, 0);

        Map<String, UUID> idByTag = new HashMap<>();
        awaiting.forEach(a -> idByTag.put(a.getTrackingId().toString(), a.getId()));
        LocalDate from = awaiting.stream().map(EmailQueueRepository.AwaitingDelivery::getSentAt)
                .min(Comparator.naturalOrder()).orElse(now).toLocalDate();
        LocalDate to = now.toLocalDate();

        int delivered = 0, bounced = 0, page = 0;
        boolean hasNext = true;
        while (hasNext && !idByTag.isEmpty() && page < MAX_PAGES) {
            page++;
            ReportPage result = client.messages(from, to, page);
            for (ReportedMessage m : result.messages()) {
                UUID id = m.trackingTag() == null ? null : idByTag.get(m.trackingTag());
                if (id == null) continue;
                if (m.isBounced()) {
                    bounced += repository.markBounced(id, local(m.bouncedAt()), reason(m));
                    idByTag.remove(m.trackingTag());
                } else if (m.isDelivered() && m.createdAt() != null) {
                    delivered += repository.markDelivered(id, local(m.createdAt()));
                    idByTag.remove(m.trackingTag());
                }
                // Outro status (ainda na fila da Locaweb): fica para a próxima passada.
            }
            hasNext = result.hasNext();
        }
        if (page == MAX_PAGES && hasNext && !idByTag.isEmpty()) {
            LOGGER.warn("Rastreio de entrega parou em {} páginas com {} e-mails sem achar", MAX_PAGES, idByTag.size());
        }
        return new Result(awaiting.size(), delivered, bounced, page);
    }

    /** A Locaweb responde com fuso (-03:00); a fila guarda a hora local da aplicação. */
    private LocalDateTime local(OffsetDateTime t) {
        return t.atZoneSameInstant(clock.getZone()).toLocalDateTime();
    }

    private static String reason(ReportedMessage m) {
        String d = m.bounceDescription() == null ? "" : m.bounceDescription().trim();
        if (!d.isEmpty()) return d;
        return m.bounceCode() == null ? "Devolvido pelo servidor do destinatário" : "Devolvido, código " + m.bounceCode();
    }
}
