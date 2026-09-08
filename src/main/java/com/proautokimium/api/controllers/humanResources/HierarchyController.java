package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.HierarchyResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.HierarchyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/organizational-structure:ALTERAR')")
    public ResponseEntity<HierarchyResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHierarchyRequestDTO request
    ) {
        return ResponseEntity.ok(hierarchyService.update(id, request));
    }

    /**
     * Recusa com 409 quando o cadastro esta em uso, e a mensagem diz por quem —
     * o front mostra essa frase inteira.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/organizational-structure:EXCLUIR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        hierarchyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
