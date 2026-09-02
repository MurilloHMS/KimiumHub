package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.signature.BackgroundResponseDTO;
import com.proautokimium.api.Application.DTOs.signature.TemplateResponseDTO;
import com.proautokimium.api.Application.DTOs.signature.TemplateUpdateDTO;
import com.proautokimium.api.Infrastructure.services.signature.EmailSignatureTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("api/email/signature/template")
public class EmailSignatureTemplateController {

    private final EmailSignatureTemplateService service;

    public EmailSignatureTemplateController(EmailSignatureTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('communication/email-signature:CONSULTAR')")
    public ResponseEntity<TemplateResponseDTO> buscar() {
        return ResponseEntity.ok(service.find());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('communication/email-signature:CONFIGURAR')")
    public ResponseEntity<TemplateResponseDTO> salvar(
            Authentication authentication,
            @RequestBody @Valid TemplateUpdateDTO dto) {
        return ResponseEntity.ok(service.save(dto.document(), authentication.getName()));
    }

    @PostMapping("/background")
    @PreAuthorize("hasAuthority('communication/email-signature:CONFIGURAR')")
    public ResponseEntity<BackgroundResponseDTO> enviarFundo(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.sendBackground(file));
    }
}
