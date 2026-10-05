package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, UUID> {

    List<DocumentRequest> findAllByOrderByCreatedAtDesc();
}
