package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductInventory, UUID> {
    ProductInventory findBySystemCode(String systemCode);

}
