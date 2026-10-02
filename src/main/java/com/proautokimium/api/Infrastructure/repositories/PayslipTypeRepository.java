package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.PayslipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayslipTypeRepository extends JpaRepository<PayslipType, UUID> {

    List<PayslipType> findByActiveTrueOrderBySortOrderAscLabelAsc();

    Optional<PayslipType> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT coalesce(max(t.sortOrder), 0) FROM PayslipType t")
    int findMaxSortOrder();
}
