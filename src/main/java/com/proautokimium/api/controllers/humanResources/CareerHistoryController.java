package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CareerHistoryResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CreateCareerHistoryDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CareerHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/career-histories")
public class CareerHistoryController {
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


    private final CareerHistoryService careerHistoryService;

    public CareerHistoryController(CareerHistoryService careerHistoryService) {
        this.careerHistoryService = careerHistoryService;
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<CareerHistoryResponseDTO>> listByEmployee(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(careerHistoryService.listByEmployee(employeeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/career-structure:INCLUIR')")
    public ResponseEntity<CareerHistoryResponseDTO> create(@Valid @RequestBody CreateCareerHistoryDTO dto) {
        var created = careerHistoryService.create(dto);
        return ResponseEntity.created(URI.create("/api/hr/career-histories?employeeId=" + dto.employeeId()))
                .body(created);
    }
}
