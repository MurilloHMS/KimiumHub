package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.DocumentRequestFile;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestFileRepository extends JpaRepository<DocumentRequestFile, UUID> {

    List<DocumentRequestFile> findByDocumentRequestRecipientAndReplacedAtIsNull(DocumentRequestRecipient requestRecipient);
}
