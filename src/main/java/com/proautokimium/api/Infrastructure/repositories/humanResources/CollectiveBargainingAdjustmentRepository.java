package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.CollectiveBargainingAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollectiveBargainingAdjustmentRepository extends JpaRepository<CollectiveBargainingAdjustment, UUID> {
}
