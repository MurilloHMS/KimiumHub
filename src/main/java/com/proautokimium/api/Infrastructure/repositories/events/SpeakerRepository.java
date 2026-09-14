package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpeakerRepository extends JpaRepository<Speaker, UUID> {

    List<Speaker> findAllByOrderByNameAsc();
}
