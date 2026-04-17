package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {
    List<EmailQueue> findTop15ByStatusOrderByCreatedAtAsc(EmailStatus emailStatus);
}
