package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Position.CreatePositionRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Position.PositionResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/positions")
public class PositionController {
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


    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/career-structure:INCLUIR')")
    public ResponseEntity<PositionResponseDTO> create(@Valid @RequestBody CreatePositionRequestDTO request) {
        return ResponseEntity.ok(positionService.create(request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<PositionResponseDTO>> listAll() {
        return ResponseEntity.ok(positionService.listAll());
    }
}
