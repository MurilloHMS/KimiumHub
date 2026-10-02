package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.events.EventAnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O "Começou agora" das lives. De minuto em minuto: o lembrete diário pode
 * sair na hora cheia, mas avisar que a live começou 59 minutos depois não
 * serve para nada. A consulta é barata — só eventos online de hoje com o
 * aviso ainda não enviado.
 */
@Component
public class EventLiveStartScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EventLiveStartScheduler.class);

    private final EventAnnouncementService service;

    public EventLiveStartScheduler(EventAnnouncementService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 * * * * *", zone = "America/Sao_Paulo")
    public void run() {
        try {
            int notified = service.runLiveStarts();
            if (notified > 0) logger.info("Avisos de início de live enviados: {}", notified);
        } catch (Exception e) {
            logger.error("Falha ao processar o início das lives", e);
        }
    }
}
