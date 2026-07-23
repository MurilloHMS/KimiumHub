package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Company.CompanyResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Company.CreateCompanyRequestDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<CompanyResponseDTO> create(@Valid @RequestBody CreateCompanyRequestDTO request) {
        return ResponseEntity.ok(companyService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<CompanyResponseDTO>> listAll() {
        return ResponseEntity.ok(companyService.listAll());
    }
}
