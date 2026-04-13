package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.TemplateEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemplateEmailRepository extends JpaRepository<TemplateEmail, UUID> {
}
