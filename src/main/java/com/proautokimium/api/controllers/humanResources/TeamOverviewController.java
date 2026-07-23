package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.TeamOverview.TeamOverviewEntryDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.TeamOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/team-overview")
@Tag(name = "Visão de Equipe", description = "Painel do RH: disponibilidade e contrato de cada funcionário")
public class TeamOverviewController {

    private final TeamOverviewService service;

    public TeamOverviewController(TeamOverviewService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Visão de equipe", description = "Lista funcionários ativos com CLT/PJ e disponibilidade, filtrável por setor/empresa")
    public ResponseEntity<List<TeamOverviewEntryDTO>> get(
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID companyId
    ) {
        return ResponseEntity.ok(service.getOverview(teamId, companyId));
    }
}
