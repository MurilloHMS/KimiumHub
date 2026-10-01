package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.EventView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventViewRepository extends JpaRepository<EventView, UUID> {

    /** A linha de visualização desta pessoa neste evento, se já abriu antes. */
    Optional<EventView> findByEventIdAndEmployeeId(UUID eventId, UUID employeeId);

    /** Quem abriu o evento, para o Acompanhamento. */
    List<EventView> findByEventId(UUID eventId);
}
