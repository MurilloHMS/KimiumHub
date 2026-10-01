package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.EventResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EventResponseRepository extends JpaRepository<EventResponse, UUID> {

    /** A resposta desta pessoa a este evento, se já respondeu. */
    Optional<EventResponse> findByEventIdAndEmployeeId(UUID eventId, UUID employeeId);

    /** As respostas desta pessoa, de todos os eventos: "Meus convites" e a home. */
    List<EventResponse> findByEmployeeId(UUID employeeId);

    /** Todas as respostas de um evento, para o Acompanhamento. */
    List<EventResponse> findByEventId(UUID eventId);

    /** Quem já respondeu (vai ou não vai): o lembrete pula essas pessoas. */
    @Query("SELECT r.employee.id FROM EventResponse r WHERE r.event.id = :eventId")
    Set<UUID> findAnsweredEmployeeIds(@Param("eventId") UUID eventId);
}
