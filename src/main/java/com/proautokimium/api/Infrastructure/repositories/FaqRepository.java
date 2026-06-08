package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {
}
