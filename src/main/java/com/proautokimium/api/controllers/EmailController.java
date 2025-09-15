package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.email.SmtpEmailRequestDTO;
import com.proautokimium.api.Infrastructure.services.EmailService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.EmailEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/email")
public class EmailController {

    @Autowired
    private EmailService service;

    @PostMapping
    public ResponseEntity<Object> createEmail(@RequestBody @NotNull @Valid SmtpEmailRequestDTO dto){
        service.saveEmail(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Object> getAllEmails(){
        Set<EmailEntity> emails = service.getAll();
        return ResponseEntity.ok(emails);
    }
}
