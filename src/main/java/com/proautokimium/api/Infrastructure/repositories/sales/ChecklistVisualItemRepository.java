package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.ChecklistVisualItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistVisualItemRepository extends JpaRepository<ChecklistVisualItem, UUID> {

    List<ChecklistVisualItem> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
