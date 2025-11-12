package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.contact.ContactDTO;
import com.proautokimium.api.Infrastructure.services.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/contact")
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

    @PostMapping
    public ResponseEntity<Object> postContact(@RequestBody @NotNull @Valid ContactDTO dto){
        service.createContact(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
