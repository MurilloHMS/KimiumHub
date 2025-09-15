package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.ProductMovementRepository;
import com.proautokimium.api.domain.entities.MovementInventory;
import com.proautokimium.api.domain.entities.ProductInventory;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProductInventoryService {
    private final ProductInventoryRepository productInventoryRepository;
    private final ProductMovementRepository productMovementRepository;

    public ProductInventoryService(ProductInventoryRepository productInventoryRepository, ProductMovementRepository productMovementRepository) {
        this.productInventoryRepository = productInventoryRepository;
        this.productMovementRepository = productMovementRepository;
    }

    @Transactional
    public void saveProduct(ProductInventoryDTO dto){
        ProductInventory product = new ProductInventory();

        product.setSystemCode(dto.systemCode());
        product.setName(dto.name());
        product.setMinimumStock(dto.minimumStock());
        product.setActive(dto.active());

        productInventoryRepository.save(product);
    }

    @Transactional
    public void includeMovement(ProductMovementDTO dto){
        ProductInventory productInventory = productInventoryRepository.findBySystemCode(dto.system_code());

        MovementInventory movement = new MovementInventory();
        movement.setMovementDate(dto.movementDate());
        movement.setQuantity(dto.quantity());
        movement.setProduct(productInventory);

        productMovementRepository.save(movement);
    }
}
