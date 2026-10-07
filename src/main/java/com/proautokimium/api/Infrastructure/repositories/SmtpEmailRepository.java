package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.EmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmtpEmailRepository extends JpaRepository<EmailEntity, UUID> {
    EmailEntity findByName(String name);

    /** O remetente de quem não tem rota. */
    java.util.Optional<EmailEntity> findFirstByIsDefaultTrue();

    java.util.List<EmailEntity> findAllByOrderByNameAsc();

    boolean existsByEmail_AddressIgnoreCase(String address);
}
