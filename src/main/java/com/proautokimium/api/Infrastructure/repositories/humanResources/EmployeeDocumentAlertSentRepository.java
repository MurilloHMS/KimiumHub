package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentAlertSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface EmployeeDocumentAlertSentRepository extends JpaRepository<EmployeeDocumentAlertSent, UUID> {

    /**
     * Nome sem "Before": o Spring Data lê `...AndDaysBefore` como "o campo `days`
     * ANTES de um valor" (palavra-chave de data), não como o campo `daysBefore`.
     * O contexto nem subia. A consulta explícita tira a ambiguidade.
     */
    @Query("""
            select count(a) > 0 from EmployeeDocumentAlertSent a
             where a.documentId = :documentId
               and a.dueDate = :dueDate
               and a.daysBefore = :daysBefore
            """)
    boolean alreadySent(@Param("documentId") UUID documentId,
                        @Param("dueDate") LocalDate dueDate,
                        @Param("daysBefore") int daysBefore);
}
