package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.home.HomeSummaryDTO;
import com.proautokimium.api.Infrastructure.services.home.HomeSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/home")
@Tag(name = "Home", description = "O que está esperando o usuário logado")
public class HomeController {

    private final HomeSummaryService service;

    public HomeController(HomeSummaryService service) {
        this.service = service;
    }

    /**
     * Sem `@PreAuthorize`: todo funcionário autenticado tem uma home. O
     * `SecurityConfiguration` já nega CLIENTE em tudo que não é `api/client/**`.
     *
     * O papel sai do token aqui e desce como booleano, no mesmo padrão do
     * `HoleriteController` — assim o serviço não precisa conhecer Spring
     * Security para decidir o que devolve.
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumo da home",
               description = "Pendências do usuário logado e aprovações paradas com ele")
    public ResponseEntity<HomeSummaryDTO> summary(Authentication auth) {
        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("RH"));

        return ResponseEntity.ok(service.getSummary(auth.getName(), isRh));
    }
}
