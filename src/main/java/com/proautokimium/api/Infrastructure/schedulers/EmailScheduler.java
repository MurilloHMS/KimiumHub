package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailDispatcher;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A cada minuto, os 15 e-mails mais antigos da fila. Cada falha grava o motivo
 * (antes a exceção era descartada, nem o log a guardava) e volta para a fila
 * até a 5ª tentativa; depois fica FAILED, esperando alguém reenviar na tela.
 */
@Component
public class EmailScheduler {

    private final EmailQueueRepository repository;
    private final EmailDispatcher dispatcher;
    private final Clock clock;

    private final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);

    public EmailScheduler(EmailQueueRepository repository, EmailDispatcher dispatcher, Clock clock) {
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "0 * * * * *")
    public void processQueue() {
        List<EmailQueue> emails = repository.nextBatch(
                List.of(EmailStatus.PENDING, EmailStatus.SCHEDULED));

        for (EmailQueue email : emails) {
            try {
                dispatcher.send(email);
                email.markSent(LocalDateTime.now(clock));
            } catch (Exception ex) {
                email.recordFailure(EmailQueueService.describe(ex), LocalDateTime.now(clock));
                logger.warn("Falha ao enviar o e-mail {} (tentativa {} de {})", email.getId(), email.getAttempts(),
                        EmailQueue.MAX_ATTEMPTS, ex);
            }
            repository.save(email);
        }
    }
}
