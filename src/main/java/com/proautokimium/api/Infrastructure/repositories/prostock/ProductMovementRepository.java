package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductMovementRepository extends JpaRepository<MovementInventory, UUID> {
    @Query(value = "SELECT * FROM products_movements WHERE product_id = :id", nativeQuery = true)
    List<MovementInventory> findMovementByProductId(@Param("id") UUID id);
}
