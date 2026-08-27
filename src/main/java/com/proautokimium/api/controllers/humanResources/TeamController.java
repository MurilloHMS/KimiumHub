package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Team.CreateTeamRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.TeamResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/teams")
public class TeamController {
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


    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/organizational-structure:INCLUIR')")
    public ResponseEntity<TeamResponseDTO> create(@Valid @RequestBody CreateTeamRequestDTO request) {
        return ResponseEntity.ok(teamService.create(request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<TeamResponseDTO>> listAll() {
        return ResponseEntity.ok(teamService.listAll());
    }
}
