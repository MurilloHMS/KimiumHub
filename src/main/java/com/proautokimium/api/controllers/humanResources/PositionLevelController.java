package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.CreatePositionLevelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.PositionLevelResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionLevelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/position-levels")
public class PositionLevelController {
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


    private final PositionLevelService positionLevelService;

    public PositionLevelController(PositionLevelService positionLevelService) {
        this.positionLevelService = positionLevelService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/career-structure:INCLUIR')")
    public ResponseEntity<PositionLevelResponseDTO> create(@Valid @RequestBody CreatePositionLevelRequestDTO request) {
        return ResponseEntity.ok(positionLevelService.create(request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<PositionLevelResponseDTO>> listByPosition(@RequestParam UUID positionId) {
        return ResponseEntity.ok(positionLevelService.listByPosition(positionId));
    }

    /**
     * Altera o nivel — inclusive o valor base.
     *
     * Permissao de `rh/career-structure`, e nao da Estrutura: quem organiza a
     * empresa nao e necessariamente quem mexe em salario.
     *
     * A resposta traz o `resolvedSalary` deste nivel recalculado; os demais
     * niveis do cargo tambem mudam, porque nivel percentual resolve sobre o
     * anterior. Quem chama relê a lista.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/career-structure:ALTERAR')")
    public ResponseEntity<PositionLevelResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePositionLevelRequestDTO request
    ) {
        return ResponseEntity.ok(positionLevelService.update(id, request));
    }
}
