package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.domain.entities.ProductInventory;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {
    ProductInventory findBySystemCode(String systemCode);
    List<ProductInventory> findBySystemCodeIn(List<String> systemCode);

    @Query("""
        SELECT p
        FROM ProductInventory p
        JOIN p.movements m
        WHERE m.movementDate = (
            SELECT MAX(m2.movementDate)
            FROM MovementInventory m2
            WHERE m2.product = p
        )
        AND m.quantity < p.minimumStock
    """)
    List<ProductInventory> findProductByMovementBelowMinimum();


}
