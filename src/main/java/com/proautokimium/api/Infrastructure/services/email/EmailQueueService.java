package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import org.springframework.stereotype.Service;

@Service
public class EmailQueueService {

    private final EmailQueueRepository repository;

    public EmailQueueService(EmailQueueRepository repository) {
        this.repository = repository;
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
}
