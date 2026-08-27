package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.profile.*;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.services.vcard.ProfileService;
import com.proautokimium.api.Infrastructure.services.vcard.VCardService;
import com.proautokimium.api.domain.entities.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class VCardController {

    private final VCardService vCardService;
    private final ProfileService service;
    private final ProfileConverter converter;

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProfileResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProfileResponseDto> create(@RequestBody ProfileCreateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponseDto> update(
            @PathVariable UUID id,
            @RequestBody ProfileUpdateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Self-service (Meu Perfil) ─────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponseDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(service.getMyProfile(authentication.getName()));
    }

    /**
     * Criar o próprio cartão.
     *
     * Era `hasRole('VENDEDOR')`, e a V88 moveu a decisão para a permissão. Se
     * este endpoint tivesse ficado na role, a tela mentiria: ela passou a
     * mostrar o botão por `perfil:INCLUIR`, e quem tivesse a permissão sem a
     * role veria o botão e levaria 403 ao clicar.
     */
    @PostMapping("/me")
    @PreAuthorize("hasAuthority('perfil:INCLUIR')")
    public ResponseEntity<ProfileResponseDto> createMyProfile(
            Authentication authentication,
            @RequestBody ProfileCreateDto dto) {
        return ResponseEntity.ok(service.createMyProfile(authentication.getName(), dto));
    }

    /** Editar o próprio cartão — quem cria, mantém. */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('perfil:ALTERAR')")
    public ResponseEntity<ProfileResponseDto> updateMyProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateDto dto) {
        return ResponseEntity.ok(service.updateMyProfile(authentication.getName(), dto));
    }

    /** A foto é parte do cartão, e segue a mesma permissão de editá-lo. */
    @PostMapping("/me/image")
    @PreAuthorize("hasAuthority('perfil:ALTERAR')")
    public ResponseEntity<String> uploadMyProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = service.uploadMyProfileImage(authentication.getName(), file);
        return ResponseEntity.ok(imageUrl);
    }

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/public/{slug}")
    public ResponseEntity<ProfileResponseDto> getBySlug(@PathVariable String slug) {
        Optional<ProfileResponseDto> profile = service.findBySlug(slug).map(converter::toDto);
        return ResponseEntity.ok(profile.get());
    }

    @GetMapping("/public/{slug}/vcard")
    public ResponseEntity<byte[]> downloadVCard(@PathVariable String slug) {
        Optional<Profile> profile = service.findBySlug(slug);

        if (profile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = vCardService.generate(profile.get());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + profile.get().getSlug() + ".vcf\"")
                .contentType(MediaType.parseMediaType("text/vcard"))
                .body(content);
    }
}