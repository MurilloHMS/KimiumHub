package com.proautokimium.api.Infrastructure.repositories.email.signature;

import com.proautokimium.api.domain.entities.EmailSignatureTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailSignatureTemplateRepository extends JpaRepository<EmailSignatureTemplate, UUID> {
    Optional<EmailSignatureTemplate> findFirstByOrderByUpdatedAtDesc();
}
