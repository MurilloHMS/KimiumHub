package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.HrDashboardSummaryDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.HrDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/dashboard-summary")
@Tag(name = "Painel RH", description = "KPIs agregados de funcionários, folha e estrutura organizacional")
public class HrDashboardController {

    private final HrDashboardService service;

    public HrDashboardController(HrDashboardService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('rh/hub:CONSULTAR')")
    @GetMapping
    @Operation(summary = "Resumo do painel", description = "Funcionários por empresa (com CLT/PJ), por cargo, por departamento, folha total e tamanho da estrutura organizacional")
    public ResponseEntity<HrDashboardSummaryDTO> summary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
