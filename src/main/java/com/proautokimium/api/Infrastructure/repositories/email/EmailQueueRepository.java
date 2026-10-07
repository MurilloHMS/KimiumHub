package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID>, JpaSpecificationExecutor<EmailQueue> {
    List<EmailQueue> findTop15ByStatusOrderByCreatedAtAsc(EmailStatus emailStatus);

    /** O lote do agendador, já com os anexos: o envio acontece fora de transação. */
    @EntityGraph(attributePaths = "attachments")
    List<EmailQueue> findTop15ByStatusInOrderByCreatedAtAsc(List<EmailStatus> statuses);

    // ── Para a tela do desenvolvedor: contagens, sem carregar o corpo dos e-mails ──

    interface StatusCount { EmailStatus getStatus(); long getTotal(); }

    @Query("SELECT e.status AS status, COUNT(e) AS total FROM EmailQueue e WHERE e.createdAt >= :since GROUP BY e.status")
    List<StatusCount> countByStatusSince(@Param("since") LocalDateTime since);

    interface DayCount { java.time.LocalDate getDay(); EmailStatus getStatus(); long getTotal(); long getRetried(); }

    @Query("""
            SELECT CAST(e.createdAt AS LocalDate) AS day, e.status AS status, COUNT(e) AS total,
                   SUM(CASE WHEN e.attempts > 1 THEN 1 ELSE 0 END) AS retried
            FROM EmailQueue e WHERE e.createdAt >= :since
            GROUP BY CAST(e.createdAt AS LocalDate), e.status
            """)
    List<DayCount> countByDaySince(@Param("since") LocalDateTime since);

    interface OriginCount { com.proautokimium.api.domain.enums.email.EmailOrigin getOrigin(); EmailStatus getStatus(); long getTotal(); }

    @Query("SELECT e.origin AS origin, e.status AS status, COUNT(e) AS total FROM EmailQueue e WHERE e.createdAt >= :since GROUP BY e.origin, e.status")
    List<OriginCount> countByOriginSince(@Param("since") LocalDateTime since);

    @Query("SELECT e.lastError FROM EmailQueue e WHERE e.status = :status AND e.createdAt >= :since")
    List<String> errorsSince(@Param("status") EmailStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(e) FROM EmailQueue e WHERE e.status = :status AND e.attempts > 1 AND e.createdAt >= :since")
    long countRetriedSince(@Param("status") EmailStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(AVG(e.attempts), 0) FROM EmailQueue e WHERE e.status IN :statuses AND e.createdAt >= :since")
    double averageAttemptsSince(@Param("statuses") List<EmailStatus> statuses, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(e) FROM EmailQueue e WHERE e.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<EmailStatus> statuses);

    @Query("SELECT MIN(e.createdAt) FROM EmailQueue e WHERE e.status IN :statuses")
    LocalDateTime oldestCreatedAt(@Param("statuses") List<EmailStatus> statuses);

    @Query("SELECT MAX(COALESCE(e.lastAttemptAt, e.sentAt)) FROM EmailQueue e")
    LocalDateTime lastActivityAt();

    @Query("SELECT e.origin AS origin, e.status AS status, COUNT(e) AS total FROM EmailQueue e WHERE e.createdAt >= :since GROUP BY e.origin, e.status")
    List<OriginCount> countForRoutes(@Param("since") LocalDateTime since);
}
