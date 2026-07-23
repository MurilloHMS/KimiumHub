package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Announcement.AnnouncementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Announcement.CreateAnnouncementRequestDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/announcements")
@Tag(name = "Mural de Avisos", description = "Avisos publicados pelo RH pra todos os funcionários")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Publica aviso", description = "Publica no mural e notifica todos os funcionários ativos")
    public ResponseEntity<AnnouncementResponseDTO> publish(@Valid @RequestBody CreateAnnouncementRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.publish(request));
    }

    @GetMapping
    @Operation(summary = "Lista avisos", description = "Mural completo, mais recente primeiro — aberto a qualquer funcionário autenticado")
    public ResponseEntity<List<AnnouncementResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }
}
