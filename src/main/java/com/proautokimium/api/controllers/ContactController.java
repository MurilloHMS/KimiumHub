package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.contact.CreateContactDTO;
import com.proautokimium.api.Infrastructure.services.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/contact")
@Tag(name = "Contato empresa", description = "Cria e retorna os dados de contato da empresa")
public class ContactController {

    @Autowired
    ContactService service;

    @GetMapping
    public ResponseEntity<Object> getAllContacts(){
        var contacts = service.getAllContact();
        
        if(contacts == null || contacts.isEmpty())
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foram encontrados registros de contato!");
        
        return ResponseEntity.ok(contacts);
    }

    /**
     * Cria um registro de contato.
     * @param dto CreateContactDTO - Dto para criação de um novo registro de contato.
     * @return HttpStatus Created (201)
     */
    @PostMapping
    @Operation(summary = "Registra um contato", description = "Recebe os dados de contato e registra")
    public ResponseEntity<Object> create(@RequestBody @NotNull @Valid CreateContactDTO dto){
        service.createContact(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
