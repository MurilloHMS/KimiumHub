package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.services.ProductInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("api/product")
public class ProductController {

    @Autowired
    private ProductInventoryService inventoryService;

    @PostMapping("/inventory/product")
    public ResponseEntity<Object> createInventoryProduct(@RequestBody @NotNull @Valid ProductInventoryDTO dto){
        inventoryService.saveProduct(dto);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/inventory/movement")
    public ResponseEntity<Object> createInventoryMovement(@RequestBody @NotNull @Valid ProductMovementDTO dto){
        inventoryService.includeMovement(dto);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/inventory/product")
    public ResponseEntity<Object> getAllProducts(){
        var products = inventoryService.findAllProducts();
        return ResponseEntity.ok().body(products);
    }

    @GetMapping("/inventory/movements/{systemCode}")
    public ResponseEntity<List<ProductMovementDTO>> getAllMovementsBySystemCode(@PathVariable String systemCode){
        List<ProductMovementDTO> movements = inventoryService.findAllMovementsByProduct(systemCode);
        return ResponseEntity.ok(movements);
    }
}
