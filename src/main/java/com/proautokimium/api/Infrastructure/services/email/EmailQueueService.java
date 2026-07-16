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
        EmailQueue email = new EmailQueue(
                to,
                from,
                subject,
                body);
        try{
            emailService.sendEmail(email);
            email.markEmailSent();
        }catch (Exception e){
            email.setStatus(EmailStatus.FAILED);
            throw e;
        }finally {
            repository.save(email);
        }
    }
}
