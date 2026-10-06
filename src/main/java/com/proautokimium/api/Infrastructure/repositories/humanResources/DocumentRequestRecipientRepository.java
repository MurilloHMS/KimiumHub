package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestRecipientRepository extends JpaRepository<DocumentRequestRecipient, UUID> {
    List<DocumentRequestRecipient> findByDocumentRequestOrderByAddedAtDesc(DocumentRequest request);

    List<DocumentRequestRecipient> findByStatus(RecipientStatus status);

    /** As solicitações de um funcionário, a mais nova primeiro. Rascunho nunca chega aqui: destinatário só nasce no envio. */
    List<DocumentRequestRecipient> findByEmployeeOrderByAddedAtDesc(Employee employee);

    /**
     * Quantos destinatários em cada status, por solicitação: uma consulta para a
     * lista inteira do RH, em vez de carregar todas as respostas para contar.
     */
    @Query("""
            SELECT r.documentRequest.id AS requestId, r.status AS status, COUNT(r) AS total
            FROM DocumentRequestRecipient r
            GROUP BY r.documentRequest.id, r.status
            """)
    List<StatusCount> countByRequestAndStatus();

    interface StatusCount {
        UUID getRequestId();
        RecipientStatus getStatus();
        long getTotal();
    }
}
