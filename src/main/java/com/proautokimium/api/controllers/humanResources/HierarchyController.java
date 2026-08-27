package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.HierarchyResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.HierarchyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/hierarchies")
public class HierarchyController {
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
            + "'rh/career-structure:CONSULTAR', 'rh/employees:CONSULTAR')";


    private final HierarchyService hierarchyService;

    public HierarchyController(HierarchyService hierarchyService) {
        this.hierarchyService = hierarchyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/organizational-structure:INCLUIR')")
    public ResponseEntity<HierarchyResponseDTO> create(@Valid @RequestBody CreateHierarchyRequestDTO request) {
        return ResponseEntity.ok(hierarchyService.create(request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<HierarchyResponseDTO>> listAll() {
        return ResponseEntity.ok(hierarchyService.listAll());
    }
}
