package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Application.DTOs.prostock.product.ProductInventoryDTO;
import com.proautokimium.api.Application.DTOs.prostock.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.services.inventoryProducts.ProductInventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("api/inventory")
public class ProductController {
    /**
     * As leituras que alimentam os stores compartilhados.
     *
     * O Hub, as Movimentações e a Programação leem as mesmas máquinas e os
     * mesmos registros — os stores são um só. Exigir uma tela específica
     * aqui esvaziaria as outras duas sem erro nenhum na tela.
     */
    private static final String LER_ESTOQUE =
            "hasAnyAuthority('stock/hub:CONSULTAR', 'stock/programacao:CONSULTAR', "
            + "'stock/movements:CONSULTAR', 'stock/inventory-hub:CONSULTAR')";


    @Autowired
    private ProductInventoryService inventoryService;

    @PreAuthorize("hasAuthority('stock/products:INCLUIR')")
    @PostMapping("product")
    public ResponseEntity<Object> createInventoryProduct(@RequestBody @NotNull @Valid ProductInventoryDTO dto){
        inventoryService.saveProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAuthority('stock/movements:INCLUIR')")
    @PostMapping("movement")
    public ResponseEntity<Object> createInventoryMovement(@RequestBody @NotNull @Valid ProductMovementDTO dto){
    	if(dto.systemCode() != null) {
    		inventoryService.includeMovement(dto);
            return ResponseEntity.status(HttpStatus.CREATED).build();
    	}else {
    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Movimentação não incluida, código do sistema está nulo ou vazio");
    	}
    }

    @PreAuthorize(LER_ESTOQUE)
    @GetMapping("product")
    public ResponseEntity<Object> getAllProducts(){
        var products = inventoryService.findAllProducts();
        return ResponseEntity.ok().body(products);
    }

    @PreAuthorize("hasAuthority('stock/inventory-hub:CONSULTAR')")
    @GetMapping("product/lowstock")
    public ResponseEntity<?> getAllProductsWithLowStock(){
        return inventoryService.getProductWithLowStock();
    }

    @PreAuthorize(LER_ESTOQUE)
    @GetMapping("movements/{systemCode}")
    public ResponseEntity<List<ProductMovementDTO>> getAllMovementsBySystemCode(@PathVariable String systemCode){
        List<ProductMovementDTO> movements = inventoryService.findAllMovementsByProduct(systemCode);
        return ResponseEntity.ok(movements);
    }

    @PreAuthorize("hasAuthority('stock/products:EXCLUIR')")
    @DeleteMapping("product/{systemCode}")
    public ResponseEntity<Object> deleteProductById(@PathVariable String systemCode){
        inventoryService.deleteProductBySystemCode(systemCode);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('stock/products:ALTERAR')")
    @PutMapping("product")
    public ResponseEntity<Object> updateProduct(@RequestBody @NotNull @Valid ProductInventoryDTO dto){
        inventoryService.updateProduct(dto);
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("hasAuthority('stock/products:INCLUIR')")
    @PostMapping("product/upload")
    public ResponseEntity<Object> createProductsBySheet(@RequestParam MultipartFile file) throws Exception{
    	ResponseEntity<Object> response = inventoryService.includeProductBySheet(file);
    	return response;
    }
    
    @PreAuthorize("hasAuthority('stock/movements:BAIXAR')")
    @GetMapping("movements/reports/{date}")
    public ResponseEntity<Object> getMovementsByDate(@PathVariable LocalDate date){
    	if(date == null)
    		ResponseEntity.status(HttpStatus.NO_CONTENT).body("Data inválida ou nula");
    	
    	var response = inventoryService.getMovementsByDate(date);
    	return response;
    }
}
