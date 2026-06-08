package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentCreateDTO;
import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentUpdateDTO;
import com.proautokimium.api.Infrastructure.services.guide.EquipmentGuideService;
import com.proautokimium.api.Infrastructure.exceptions.file.FailedStorageFileException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Controller responsável pelo cadastro dos Equipamentos do "Guia de Utilização".
 *
 *
 */
@RestController
@RequestMapping("/api/product/website/equipment")
@Tag(name = "Crud", description = "Cadastros dos equipamentos do guia")
public class ProductEquipmentController {

    @Autowired
    private EquipmentGuideService service;

    @GetMapping
    public ResponseEntity<Object> getAll(){
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestPart("data") ProductEquipmentCreateDTO dto,
                                         @RequestPart("image")MultipartFile file){
        try {
            service.create(dto, file);
        } catch (IOException e) {
            throw new FailedStorageFileException();
        }
        return ResponseEntity.ok().body("Equipamento cadastrado com sucesso!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") UUID uuid,
                                         @RequestPart("data")ProductEquipmentUpdateDTO dto,
                                         @RequestPart("image") MultipartFile file) {
        try{
            service.update(uuid,dto, file);
        }catch (IOException e){
            throw new FailedStorageFileException();
        }
        return ResponseEntity.ok().body("Equipamento atualizado com sucesso!");
    }
}
