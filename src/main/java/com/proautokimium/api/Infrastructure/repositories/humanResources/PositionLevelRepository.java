package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionLevelRepository extends JpaRepository<PositionLevel, UUID> {
    Optional<PositionLevel> findByPositionAndLevelOrder(Position position, Integer levelOrder);
    List<PositionLevel> findByPositionOrderByLevelOrderAsc(Position position);
    List<PositionLevel> findByAdjustmentType(SalaryAdjustmentType adjustmentType);
    List<PositionLevel> findByPositionAndAdjustmentType(Position position, SalaryAdjustmentType adjustmentType);
}
