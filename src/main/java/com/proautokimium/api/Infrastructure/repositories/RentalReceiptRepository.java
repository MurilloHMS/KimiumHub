package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.RentalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RentalReceiptRepository extends JpaRepository<RentalReceipt, UUID> {

    List<RentalReceipt> findByBatchIdOrderByCreatedAt(UUID batchId);
}
