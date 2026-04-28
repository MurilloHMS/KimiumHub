package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.secrets.CreateSecretRequestDTO;
import com.proautokimium.api.Application.DTOs.secrets.CreateSecretResponseDTO;
import com.proautokimium.api.Application.DTOs.secrets.SecretContentResponseDTO;
import com.proautokimium.api.Infrastructure.services.secrets.PublicSecretService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public-secrets")
@RequiredArgsConstructor
public class PublicSecretController {

    @Autowired
    private final PublicSecretService service;
    @Value("${app.base-url}") private String baseUrl;

    @PostMapping
    public ResponseEntity<CreateSecretResponseDTO> create (@Valid @RequestBody CreateSecretRequestDTO req) throws Exception{
        String token = service.create(req.content());
        return ResponseEntity.ok(new CreateSecretResponseDTO(baseUrl + "/s/" + token));
    }

    @GetMapping("/{token}")
    public ResponseEntity<SecretContentResponseDTO> consume(@PathVariable String token) throws Exception{
        return ResponseEntity.ok(new SecretContentResponseDTO(service.consume(token)));
    }
}
