package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.ChecklistEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistEventRepository extends JpaRepository<ChecklistEvent, UUID> {

    List<ChecklistEvent> findByChecklistIdOrderByCreatedAtAsc(UUID checklistId);
}
