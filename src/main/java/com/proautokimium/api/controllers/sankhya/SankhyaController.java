package com.proautokimium.api.controllers.sankhya;

import com.proautokimium.api.Application.DTOs.sankhya.SankhyaQueryDTO;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sankhya/")
public class SankhyaController {

    private final SankhyaQueryService queryService;

    public SankhyaController(SankhyaQueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping(value = "query", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('integracao/sankhya:CONSULTAR')")
    public ResponseEntity<String> query(@RequestBody @Valid SankhyaQueryDTO dto){
        String result = queryService.query(dto.query());
        return ResponseEntity.ok(result);
    }
}
