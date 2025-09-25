package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.contact.ContactDTO;
import com.proautokimium.api.Infrastructure.services.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/contact")
public class ContactController {

    @Autowired
    ContactService service;

    @GetMapping
    public ResponseEntity<Object> getAllContacts(){
        var contacts = service.getAllContact();
        return ResponseEntity.ok(contacts);
    }

    @PostMapping
    public ResponseEntity<Object> postContact(ContactDTO dto){
        service.createContact(dto);
        return ResponseEntity.status(201).build();
    }
}
