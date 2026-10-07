package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.email.delivery.EmailDeliveryTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A cada 10 minutos, pergunta à Locaweb quais e-mails chegaram.
 *
 * <p>A falha da API (fora do ar, token trocado) só vai para o log: a entrega é
 * informação, e o e-mail já saiu. A próxima passada tenta de novo.
 */
@Component
public class EmailDeliveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeliveryScheduler.class);

    private final EmailDeliveryTrackingService tracking;

    public EmailDeliveryScheduler(EmailDeliveryTrackingService tracking) {
        this.tracking = tracking;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void track() {
        try {
            EmailDeliveryTrackingService.Result r = tracking.track();
            if (r.awaiting() > 0) {
                LOGGER.info("Rastreio de entrega: {} esperando, {} entregues, {} devolvidos ({} página(s))",
                        r.awaiting(), r.delivered(), r.bounced(), r.pages());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Rastreio de entrega falhou; tenta de novo na próxima passada", e);
        }
    }
}
