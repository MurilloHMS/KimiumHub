package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.ChecklistComodatoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistComodatoItemRepository extends JpaRepository<ChecklistComodatoItem, UUID> {

    List<ChecklistComodatoItem> findAllByOrderBySortOrderAsc();

    boolean existsByProductCode(int productCode);
}
