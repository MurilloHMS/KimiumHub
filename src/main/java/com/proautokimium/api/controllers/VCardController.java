package com.proautokimium.api.controllers;

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

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Profile> create(@RequestBody Profile profile) {
        return ResponseEntity.ok(service.create(profile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profile> update(@PathVariable UUID id, @RequestBody Profile profile) {
        return ResponseEntity.ok(service.update(id, profile));
    }

    @GetMapping("/{slug}/vcard")
    public ResponseEntity<byte[]> download(@PathVariable String slug) {
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