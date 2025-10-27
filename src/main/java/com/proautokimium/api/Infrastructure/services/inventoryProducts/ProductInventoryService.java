package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.ProductMovementRepository;
import com.proautokimium.api.domain.entities.MovementInventory;
import com.proautokimium.api.domain.entities.ProductInventory;
import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductInventoryService {
    private final ProductInventoryRepository productInventoryRepository;
    private final ProductMovementRepository productMovementRepository;
    private final InventoryProductExcelReaderService reader;

    public ProductInventoryService(ProductInventoryRepository productInventoryRepository, ProductMovementRepository productMovementRepository, InventoryProductExcelReaderService reader) {
        this.productInventoryRepository = productInventoryRepository;
        this.productMovementRepository = productMovementRepository;
		this.reader = reader;
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
    
    @Transactional
    public ResponseEntity<Object> includeProductBySheet(MultipartFile file){
    	try {
    		List<ProductInventory> products = reader.getDataByExcel(file.getInputStream());
    		
    		if(products.isEmpty()) {
    			return ResponseEntity.badRequest().body("Nenhum produto encontrado no arquivo");
    		}
    		
    		List<String> systemCodes = products.stream().map(ProductInventory::getSystemCode).toList();
    		
    		List<ProductInventory> existingProducts = productInventoryRepository.findBySystemCodeIn(systemCodes);
    		
    		Map<String, ProductInventory> existingMap = existingProducts.stream().collect(Collectors.toMap(ProductInventory::getSystemCode, c -> c));
    		
    		List<ProductInventory> toInsert = new ArrayList<>();
    		List<ProductInventory> toUpdate = new ArrayList<>();
    		
    		for(ProductInventory p : products) {
    			if(existingMap.containsKey(p.getSystemCode())) {
    				
    				ProductInventory existing = existingMap.get(p.getSystemCode());
    				existing.setName(p.getName());
    				toUpdate.add(existing);
    			}else {
					toInsert.add(p);
				}
    		}
    		
    		if(!toInsert.isEmpty())
    			productInventoryRepository.saveAll(toInsert);
    		
    		if(!toUpdate.isEmpty())
    			productInventoryRepository.saveAll(toUpdate);
    		
    		return ResponseEntity.ok(String.format(
    				"%d produtos adicionados, %d atualizados",
    				toInsert.size(), toUpdate.size()
				));
    	}catch (Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Erro ao processar arquivo: " + e.getMessage());
		}
    }
}
