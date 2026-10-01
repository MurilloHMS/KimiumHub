package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.EventReminderSent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventReminderSentRepository extends JpaRepository<EventReminderSent, UUID> {

    /** Já saiu lembrete deste evento hoje? (A chave única da V113 é a garantia; isto evita a tentativa.) */
    boolean existsByEventIdAndSentOn(UUID eventId, LocalDate sentOn);

    /** Os dias em que o lembrete saiu, para o Acompanhamento. */
    List<EventReminderSent> findByEventIdOrderBySentOnAsc(UUID eventId);
}
