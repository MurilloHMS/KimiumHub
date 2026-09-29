package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Os avisos de vencimento de documento, todo dia às 8h.
 *
 * Horário fixo, e não configurável como na Programação: lá a hora é da tela de
 * alertas; aqui o que o RH configura é o prazo e quem recebe, por tipo. O fuso
 * vai no cron porque o contêiner roda em UTC — sem ele, 8h seriam 5h aqui.
 */
@Slf4j
@Component
public class EmployeeDocumentAlertScheduler {

    private final EmployeeDocumentAlertService service;

    public EmployeeDocumentAlertScheduler(EmployeeDocumentAlertService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    public void run(){
        try{
            int alerted = service.runAlerts();
            if(alerted > 0) log.info("Avisos de vencimento de documento enviados: {}", alerted);
        }catch (Exception e){
            log.error("Falha ao processar avisos de vencimento de documento", e);
        }
    }
}
