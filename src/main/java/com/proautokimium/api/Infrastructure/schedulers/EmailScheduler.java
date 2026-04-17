package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmailScheduler {

    @Autowired
    private EmailQueueRepository repository;

    @Autowired
    private SmtpService emailService;

    private final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);

    @Scheduled(cron = "0 * * * * *")
    public void processQueue() {

        logger.info("Obtendo lista de emails");
        List<EmailQueue> emails = repository
                .findTop15ByStatusOrderByCreatedAtAsc(EmailStatus.PENDING);

        if(emails == null || emails.isEmpty()){
            logger.info("Não há emails pendentes de envio");
            return;
        }

        for (EmailQueue email : emails) {

            try {
                logger.info("Iniciando tentativa {} de 05 envios", email.getAttempts() + 1);
                emailService.sendEmail(email);

                email.setStatus(EmailStatus.SENT);
                email.setSentAt(LocalDateTime.now());
                logger.info("Email enviado com sucesso!");
            } catch (Exception ex) {

                email.setAttempts(email.getAttempts() + 1);

                if (email.getAttempts() >= 5) {
                    email.setStatus(EmailStatus.FAILED);
                    logger.error("O limite de tentativas de envio foram excedidas");
                } else {
                    email.setStatus(EmailStatus.PENDING);
                    logger.warn("Falha ao enviar o email");
                }
            }

            repository.save(email);
        }
    }
}
