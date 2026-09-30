package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.ChecklistVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChecklistVersionRepository extends JpaRepository<ChecklistVersion, UUID> {

    Optional<ChecklistVersion> findByChecklistIdAndVersion(UUID checklistId, int version);
}
