package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.profile.ProfileCreateDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileUpdateDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.services.vcard.ProfileService;
import com.proautokimium.api.Infrastructure.services.vcard.VCardService;
import com.proautokimium.api.domain.entities.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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