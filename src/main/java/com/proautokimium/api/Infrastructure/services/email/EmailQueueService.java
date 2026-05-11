package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.stereotype.Service;

@Service
public class EmailQueueService {

    private final EmailQueueRepository repository;

    public EmailQueueService(EmailQueueRepository repository) {
        this.repository = repository;
    }

    public EmailQueue create(EmailQueue email) {
        email.setStatus(EmailStatus.SCHEDULED);
        return repository.save(email);
    }
}
