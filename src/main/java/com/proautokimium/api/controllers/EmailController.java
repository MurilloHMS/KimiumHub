package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.email.SmtpEmailRequestDTO;
import com.proautokimium.api.Infrastructure.services.email.EmailService;
import com.proautokimium.api.domain.entities.EmailEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Responsável pelo cadastro dos e-mails utilizados nos envios SMTP
 */
@RestController
@RequestMapping("api/email")
@Tag(name = "Cria emails SMTP", description = "Cria emails para utilizar no disparo smtp")
public class EmailController {

    @Autowired
    private EmailService service;

    /**
     * Cria endereço de e-mail para envios SMTP
     * @param dto Recebe nome do e-mail
     * @return HttpStatus Created (201)
     */
    @PostMapping
    @Operation(summary = "Cria email", description = "Cria email para envio SMTP")
    public ResponseEntity<Object> createEmail(@RequestBody @NotNull @Valid SmtpEmailRequestDTO dto){
        service.saveEmail(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retorna lista de e-mails disponíveis
     * @return Lista de Emails
     */
    @GetMapping
    @Operation(summary = "Obtém lista de Emails", description = "Retorna lista de e-mails disponíveis para envio SMTP")
    public ResponseEntity<Object> getAllEmails(){
        Set<EmailEntity> emails = service.getAll();
        return ResponseEntity.ok(emails);
    }
}
