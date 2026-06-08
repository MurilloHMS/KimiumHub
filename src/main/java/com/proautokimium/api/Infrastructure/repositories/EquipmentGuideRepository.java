package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.EquipmentGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório JPA para {@link EquipmentGuide}.
 */
@Repository
public interface EquipmentGuideRepository extends JpaRepository<EquipmentGuide, UUID> {
}