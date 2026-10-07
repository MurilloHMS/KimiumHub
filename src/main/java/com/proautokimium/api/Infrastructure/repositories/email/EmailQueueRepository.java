package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    int BATCH_SIZE = 15;

    /**
     * O lote do agendador, já com os anexos: o envio acontece fora de transação.
     *
     * <p><b>Em duas consultas, e não um {@code findTop15} com {@code @EntityGraph}.</b>
     * Com o fetch da coleção, o Hibernate não consegue pôr o {@code LIMIT} no
     * SQL: traz a fila inteira e corta na memória (aviso HHH90003004). Com 900
     * newsletters esperando, eram 900 corpos de HTML lidos a cada minuto para
     * usar 15. Aqui o {@code LIMIT} vai nos ids, e os anexos vêm só dos 15.
     */
    default List<EmailQueue> nextBatch(List<EmailStatus> statuses) {
        List<UUID> ids = findBatchIds(statuses, PageRequest.of(0, BATCH_SIZE));
        return ids.isEmpty() ? List.of() : findWithAttachmentsByIdInOrderByCreatedAtAsc(ids);
    }

    @Query("select e.id from EmailQueue e where e.status in :statuses order by e.createdAt asc")
    List<UUID> findBatchIds(@Param("statuses") List<EmailStatus> statuses, Pageable page);

    @EntityGraph(attributePaths = "attachments")
    List<EmailQueue> findWithAttachmentsByIdInOrderByCreatedAtAsc(List<UUID> ids);

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

    // ── Rastreio de entrega (V122) ──

    /** Só o que o agendador precisa para casar com a Locaweb: sem o corpo do e-mail. */
    interface AwaitingDelivery { UUID getId(); UUID getTrackingId(); LocalDateTime getSentAt(); }

    @Query("""
            SELECT e.id AS id, e.trackingId AS trackingId, e.sentAt AS sentAt FROM EmailQueue e
            WHERE e.status = :status AND e.trackingId IS NOT NULL AND e.deliveredAt IS NULL
              AND e.bouncedAt IS NULL AND e.sentAt >= :since
            """)
    List<AwaitingDelivery> findAwaitingDelivery(@Param("status") EmailStatus status, @Param("since") LocalDateTime since);

    /** Update direto, e não a entidade inteira: a linha é só lida pelo id, e o corpo fica no banco. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE EmailQueue e SET e.deliveredAt = :at WHERE e.id = :id AND e.deliveredAt IS NULL")
    int markDelivered(@Param("id") UUID id, @Param("at") LocalDateTime at);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE EmailQueue e SET e.bouncedAt = :at, e.bounceReason = :reason WHERE e.id = :id AND e.bouncedAt IS NULL")
    int markBounced(@Param("id") UUID id, @Param("at") LocalDateTime at, @Param("reason") String reason);

    interface DeliveryCount { Long getDone(); Long getDelivered(); Long getBounced(); }

    /** Só os rastreados: os de antes da V122 saíram sem o cabeçalho e não têm como ter entrega. */
    @Query("""
            SELECT COUNT(e) AS done,
                   SUM(CASE WHEN e.deliveredAt IS NOT NULL THEN 1 ELSE 0 END) AS delivered,
                   SUM(CASE WHEN e.bouncedAt IS NOT NULL THEN 1 ELSE 0 END) AS bounced
            FROM EmailQueue e
            WHERE e.trackingId IS NOT NULL AND e.status IN :statuses AND e.createdAt >= :since
            """)
    DeliveryCount countDeliverySince(@Param("statuses") List<EmailStatus> statuses, @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(e) FROM EmailQueue e
            WHERE e.status = :status AND e.trackingId IS NOT NULL AND e.deliveredAt IS NULL
              AND e.bouncedAt IS NULL AND e.createdAt >= :since
            """)
    long countAwaitingDeliverySince(@Param("status") EmailStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT e.origin AS origin, e.status AS status, COUNT(e) AS total FROM EmailQueue e WHERE e.createdAt >= :since GROUP BY e.origin, e.status")
    List<OriginCount> countForRoutes(@Param("since") LocalDateTime since);
}
