package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.machine.MachineAlertService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



@Component
public class MachineAlertScheduler {
    private final MachineAlertService service;
    private final Logger logger = LoggerFactory.getLogger(MachineAlertScheduler.class);

    public MachineAlertScheduler(MachineAlertService service) { this.service = service; }

    /**
     * De hora em hora, e o serviço decide se é o horário configurado — `sendAt`
     * muda em runtime e o cron do @Scheduled é fixo na subida.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        try {
            int sent = service.runAlerts(false);
            if (sent > 0) logger.info("Alertas de saída enfileirados: {}", sent);
        } catch (Exception e) {
            logger.error("Falha ao processar alertas de saída", e);
        }
    }
}
