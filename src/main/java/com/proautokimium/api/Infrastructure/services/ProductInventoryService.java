package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.ProductMovementRepository;
import com.proautokimium.api.domain.entities.MovementInventory;
import com.proautokimium.api.domain.entities.ProductInventory;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        ProductInventory productInventory = productInventoryRepository.findBySystemCode(dto.systemCode());

        MovementInventory movement = new MovementInventory();
        movement.setMovementDate(dto.movementDate());
        movement.setQuantity(dto.quantity());
        movement.setProduct(productInventory);

        productMovementRepository.save(movement);
    }

    public Set<ProductInventory> findAllProducts(){
        var products = productInventoryRepository.findAll();
        return new HashSet<>(products);
    }

    public Set<MovementInventory> findAllMovements(){
        var movements = productMovementRepository.findAll();
        return new HashSet<>(movements);
    }

    public List<ProductMovementDTO> findAllMovementsByProduct(String systemCode){
        UUID id = productInventoryRepository.findBySystemCode(systemCode).getId();
        var movements = productMovementRepository.findMovementByProductId(id);
        return movements.stream()
                .map(m -> new ProductMovementDTO(
                        m.getMovementDate(),
                        m.getQuantity(),
                        m.getProduct().getSystemCode()
                )).toList();
    }

    @Transactional
    public void deleteProductBySystemCode(String systemCode){
        var product = productInventoryRepository.findBySystemCode(systemCode);
        if(product != null){
            productInventoryRepository.deleteById(product.id);
        }
    }

    @Transactional
    public void updateProduct(ProductInventoryDTO dto){
        var product = productInventoryRepository.findBySystemCode(dto.systemCode());

        product.setName(dto.name());
        product.setMinimumStock(dto.minimumStock());
        product.setActive(dto.active());

        productInventoryRepository.save(product);
    }
}
