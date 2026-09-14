package com.proautokimium.api.controllers.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerRequestDTO;
import com.proautokimium.api.Infrastructure.services.events.SpeakerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Palestrantes, na tela de cadastro de eventos.
 *
 * <p>Ler os palestrantes pede a tela do cadastro, e não a de Documentos: quem só
 * vê o evento recebe os palestrantes dentro das palestras.
 */
@RestController
@RequestMapping("/api/speakers")
@Tag(name = "Palestrantes", description = "Cadastro de palestrantes dos eventos")
public class SpeakerController {

    private static final String MANAGE = CompanyEventController.MANAGE;

    private final SpeakerService service;

    public SpeakerController(SpeakerService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + MANAGE + ":CONSULTAR')")
    public ResponseEntity<List<SpeakerDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + MANAGE + ":INCLUIR')")
    public ResponseEntity<SpeakerDTO> create(
            @Valid @RequestPart("data") SpeakerRequestDTO data,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(service.create(data, photo, authentication.getName()));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<SpeakerDTO> update(
            @PathVariable UUID id,
            @Valid @RequestPart("data") SpeakerRequestDTO data,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(service.update(id, data, photo, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + MANAGE + ":EXCLUIR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) throws IOException {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
