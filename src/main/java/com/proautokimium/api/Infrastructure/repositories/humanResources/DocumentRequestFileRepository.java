package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.DocumentRequestFile;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRequestFileRepository extends JpaRepository<DocumentRequestFile, UUID> {

    List<DocumentRequestFile> findByDocumentRequestRecipientAndReplacedAtIsNull(DocumentRequestRecipient requestRecipient);

    // O arquivo atual de UM campo da resposta; vazio no primeiro envio.
    Optional<DocumentRequestFile> findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(
            DocumentRequestRecipient requestRecipient, String fieldKey);

    /** Os arquivos atuais de várias respostas numa consulta só: as listas do RH e do funcionário. */
    List<DocumentRequestFile> findByDocumentRequestRecipientInAndReplacedAtIsNull(Collection<DocumentRequestRecipient> recipients);
}
