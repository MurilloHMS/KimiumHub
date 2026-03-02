package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.signature.EmailSignatureDTO;
import com.proautokimium.api.Infrastructure.services.reports.EmailSignatureService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/email/signature")
public class EmailSignatureController {

    @Autowired
    private EmailSignatureService emailSignatureService;

    @PostMapping()
    public ResponseEntity<?> postEmailSignature(@RequestBody @Valid EmailSignatureDTO dto) {

        InputStream backgroundImage = getClass()
                .getResourceAsStream("/templates/images/signature/signature.png");

        Map<String, Object> params = new HashMap<>();
        params.put("NOME", dto.nome());
        params.put("CARGO",  dto.cargo());
        params.put("CELULAR", dto.celular());
        params.put("WHATSAPP", dto.whatsapp());
        params.put("SITE",  dto.site());
        params.put("EMAIL", dto.email().getAddress());
        params.put("BACKGROUND_IMAGE", backgroundImage);
        byte[] png = emailSignatureService.generate(params, "email_signature.jrxml");

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=assinatura_email.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
