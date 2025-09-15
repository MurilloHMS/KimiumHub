package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.EmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmtpEmailRepository extends JpaRepository<EmailEntity, UUID> {
    EmailEntity findByName(String name);
}
