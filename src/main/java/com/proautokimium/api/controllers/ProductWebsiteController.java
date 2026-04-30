package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.services.product.ProductWebsiteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product/website")
public class ProductWebsiteController {

    @Autowired
    private ProductWebsiteService service;

    @GetMapping
    public ResponseEntity<List<ProductWebSiteResponseDTO>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(service.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductWebSiteResponseDTO>> getAllActiveProducts(){
        return ResponseEntity.status(HttpStatus.OK).body(service.getAllactiveProducts());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ProductWebSiteCreateDTO dto) {
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Produto cadastrado com sucesso");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestBody @Valid ProductWebSiteUpdateDTO dto, @PathVariable UUID id) {
        service.update(dto, id);
        return ResponseEntity.status(HttpStatus.OK).body("Produto atualizado com sucesso");
    }

    @PutMapping("/{id}/hide")
    public ResponseEntity<?> hideProduct(@PathVariable UUID id) {
        service.hide(id);
        return ResponseEntity.status(HttpStatus.OK).body("Produto ocultado com sucesso");
    }

    @PutMapping("/{id}/unhide")
    public ResponseEntity<?> unhideProduct(@PathVariable UUID id){
        service.unhide(id);
        return ResponseEntity.status(HttpStatus.OK).body("Produto está visível no site novamente");
    }
}
