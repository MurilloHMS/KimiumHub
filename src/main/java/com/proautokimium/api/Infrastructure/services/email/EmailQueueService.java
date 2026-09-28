package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.stereotype.Service;

@Service
public class EmailQueueService {

    private final EmailQueueRepository repository;
    private final SmtpService emailService;

    public EmailQueueService(EmailQueueRepository repository, SmtpService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public EmailQueue create(EmailQueue email) {
        email.markSchedule();
        return repository.save(email);
    }

    public EmailQueue sendEmail(String to, String from, String subject, String body){
        EmailQueue email = new EmailQueue(
                to,
                from,
                subject,
                body);
        email.markSchedule();
        return repository.save(email);
    }

    public void sendNow(String to, String from, String subject, String body){
        EmailQueue email = new EmailQueue(to, from, subject, body);
        sendAndRecord(email, () -> emailService.sendEmail(email));
    }

    /**
     * Envio imediato com anexo. A linha da fila registra que o e-mail saiu (ou
     * falhou), mas o anexo NÃO é gravado: um PDF com comprovantes pode ter
     * megabytes, e a fila não é arquivo — o documento se gera de novo.
     */
    public void sendNow(String to, String from, String subject, String body, SmtpService.Attachment attachment){
        EmailQueue email = new EmailQueue(to, from, subject, body);
        sendAndRecord(email, () -> emailService.sendWithAttachment(email, attachment));
    }

    /** SENT no sucesso; FAILED e relança na falha; grava sempre — FAILED impede o cron de reenviar. */
    private void sendAndRecord(EmailQueue email, Runnable send){
        try{
            send.run();
            email.markEmailSent();
        }catch (Exception e){
            email.setStatus(EmailStatus.FAILED);
            throw e;
        }finally {
            repository.save(email);
        }
    }
}
