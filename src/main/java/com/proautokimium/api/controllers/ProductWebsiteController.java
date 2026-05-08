package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.services.product.ProductWebsiteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public ResponseEntity<List<ProductWebSitePublicResponseDTO>> getAllActiveProducts(){
        return ResponseEntity.status(HttpStatus.OK).body(service.getAllactiveProducts());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("dados") @Valid ProductWebSiteCreateDTO dto,
            @RequestPart(value = "imagem", required = false)MultipartFile file) throws IOException {
        service.create(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body("Produto cadastrado com sucesso");
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @RequestPart("dados") @Valid ProductWebSiteUpdateDTO dto,
            @PathVariable UUID id,
            @RequestPart(value = "imagem", required = false) MultipartFile file
    ) throws IOException {
        service.update(dto, id, file);
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
