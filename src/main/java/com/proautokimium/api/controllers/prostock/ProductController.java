package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.services.inventoryProducts.ProductInventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("api/inventory")
public class ProductController {

    @Autowired
    private ProductInventoryService inventoryService;

    @PostMapping("product")
    public ResponseEntity<Object> createInventoryProduct(@RequestBody @NotNull @Valid ProductInventoryDTO dto){
        inventoryService.saveProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("movement")
    public ResponseEntity<Object> createInventoryMovement(@RequestBody @NotNull @Valid ProductMovementDTO dto){
    	if(dto.systemCode() != null) {
    		inventoryService.includeMovement(dto);
            return ResponseEntity.status(HttpStatus.CREATED).build();
    	}else {
    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Movimentação não incluida, código do sistema está nulo ou vazio");
    	}
    }

    @GetMapping("product")
    public ResponseEntity<Object> getAllProducts(){
        var products = inventoryService.findAllProducts();
        return ResponseEntity.ok().body(products);
    }

    @GetMapping("product/lowstock")
    public ResponseEntity<?> getAllProductsWithLowStock(){
        return inventoryService.getProductWithLowStock();
    }

    @GetMapping("movements/{systemCode}")
    public ResponseEntity<List<ProductMovementDTO>> getAllMovementsBySystemCode(@PathVariable String systemCode){
        List<ProductMovementDTO> movements = inventoryService.findAllMovementsByProduct(systemCode);
        return ResponseEntity.ok(movements);
    }

    @DeleteMapping("product/{systemCode}")
    public ResponseEntity<Object> deleteProductById(@PathVariable String systemCode){
        inventoryService.deleteProductBySystemCode(systemCode);
        return ResponseEntity.ok().build();
    }

    @PutMapping("product")
    public ResponseEntity<Object> updateProduct(@RequestBody @NotNull @Valid ProductInventoryDTO dto){
        inventoryService.updateProduct(dto);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("product/upload")
    public ResponseEntity<Object> createProductsBySheet(@RequestParam MultipartFile file) throws Exception{
    	ResponseEntity<Object> response = inventoryService.includeProductBySheet(file);
    	return response;
    }
    
    @GetMapping("movements/reports/{date}")
    public ResponseEntity<Object> getMovementsByDate(@PathVariable LocalDate date){
    	if(date == null)
    		ResponseEntity.status(HttpStatus.NO_CONTENT).body("Data inválida ou nula");
    	
    	var response = inventoryService.getMovementsByDate(date);
    	return response;
    }
}
