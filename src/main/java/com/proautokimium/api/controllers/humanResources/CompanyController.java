package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Company.CompanyResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Company.CreateCompanyRequestDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/companies")
public class CompanyController {
    /**
     * A leitura de referência do RH.
     *
     * Empresas, departamentos, hierarquias, times, cargos e níveis são lidos
     * pela Estrutura, por Cargos & Níveis e pelo cadastro de Funcionários —
     * os stores são compartilhados. Exigir uma tela só deixaria os combos
     * das outras vazios, sem erro nenhum na tela.
     */
    private static final String LER_ESTRUTURA_RH =
            "hasAnyAuthority('rh/organizational-structure:CONSULTAR', "
            + "'rh/career-structure:CONSULTAR', 'rh/employees:CONSULTAR', "
            // O cadastro de eventos escolhe o local entre as empresas do grupo.
            // Sem esta authority o combo de "Empresa" viria vazio para quem
            // cadastra evento sem ter tela nenhuma do RH.
            + "'communication/events:CONSULTAR')";


    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/organizational-structure:INCLUIR')")
    public ResponseEntity<CompanyResponseDTO> create(@Valid @RequestBody CreateCompanyRequestDTO request) {
        return ResponseEntity.ok(companyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/organizational-structure:ALTERAR')")
    public ResponseEntity<CompanyResponseDTO> update(@PathVariable UUID id,
                                                     @Valid @RequestBody CreateCompanyRequestDTO request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<CompanyResponseDTO>> listAll() {
        return ResponseEntity.ok(companyService.listAll());
    }
}
