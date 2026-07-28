package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.RentalReceiptBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RentalReceiptBatchRepository extends JpaRepository<RentalReceiptBatch, UUID> {

    List<RentalReceiptBatch> findAllByOrderByGeneratedAtDesc();

    List<RentalReceiptBatch> findByReferenceMonthAndReferenceYearOrderByGeneratedAtDesc(
            String referenceMonth, int referenceYear);

    List<RentalReceiptBatch> findByReferenceYearOrderByGeneratedAtDesc(int referenceYear);

    List<RentalReceiptBatch> findByReferenceMonthOrderByGeneratedAtDesc(String referenceMonth);
}
