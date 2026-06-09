package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Faq;
import com.proautokimium.api.domain.enums.StatusPostagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {
    Collection<Faq> findAllByStatus(StatusPostagem status);
}
