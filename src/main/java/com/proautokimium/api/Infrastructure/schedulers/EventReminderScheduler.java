package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.events.EventReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O lembrete dos convites. De hora em hora, e cada evento decide se é a hora
 * dele — a hora é do organizador e muda em runtime.
 */
@Component
public class EventReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EventReminderScheduler.class);

    private final EventReminderService service;

    public EventReminderScheduler(EventReminderService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "America/Sao_Paulo")
    public void run() {
        try {
            int reminded = service.runReminders();
            if (reminded > 0) logger.info("Lembretes de evento enviados: {}", reminded);
        } catch (Exception e) {
            logger.error("Falha ao processar lembretes de evento", e);
        }
    }
}
