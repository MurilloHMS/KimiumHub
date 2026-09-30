package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.ChecklistChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistChangeRepository extends JpaRepository<ChecklistChange, UUID> {

    List<ChecklistChange> findByChecklistIdOrderByChangedAtAsc(UUID checklistId);
}
