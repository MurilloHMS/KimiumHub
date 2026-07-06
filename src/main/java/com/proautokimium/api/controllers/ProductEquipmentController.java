package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentCreateDTO;
import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentUpdateDTO;
import com.proautokimium.api.Infrastructure.services.guide.EquipmentGuideService;
import com.proautokimium.api.Infrastructure.exceptions.file.FailedStorageFileException;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Equipamentos", description = "Cadastros dos equipamentos do guia")
public class ProductEquipmentController {

    @Autowired
    private EquipmentGuideService service;

    @GetMapping
    @Operation(summary = "Obtém todos os equipamentos", description = "Retorna lista com todos os equipamentos")
    public ResponseEntity<Object> getAll(){
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    @Operation(summary = "Cria um equipamento", description = "Registra um novo equipamento")
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
    @Operation(summary = "Atualiza um equipamento", description = "Atualiza um novo equipamento")
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um equipamento", description = "Exclui o registro de um equipamento")
    public ResponseEntity<Object> delete(@PathVariable("id") UUID uuid) {
        service.delete(uuid);
        return ResponseEntity.ok().body("Equipamento excluído com sucesso!");
    }
}
