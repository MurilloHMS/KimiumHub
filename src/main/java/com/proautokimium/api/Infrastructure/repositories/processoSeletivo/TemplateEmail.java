package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemplateEmail extends JpaRepository<TemplateEmail, UUID> {
}
