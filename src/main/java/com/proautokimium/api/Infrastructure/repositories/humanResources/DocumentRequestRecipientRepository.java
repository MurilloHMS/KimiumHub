package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestRecipientRepository extends JpaRepository<DocumentRequestRecipient, UUID> {
    List<DocumentRequestRecipient> findByDocumentRequestOrderByAddedAtDesc(DocumentRequest request);

    List<DocumentRequestRecipient> findByStatus(RecipientStatus status);
}
