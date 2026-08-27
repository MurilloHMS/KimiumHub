package com.proautokimium.api.controllers.gallery;

import com.proautokimium.api.Application.DTOs.gallery.CreateGalleryDocumentDTO;
import com.proautokimium.api.Application.DTOs.gallery.GalleryDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.services.gallery.GalleryDocumentService;
import com.proautokimium.api.domain.entities.gallery.GalleryDocument;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/gallery")
public class GalleryController {

    private final GalleryDocumentService galleryDocumentService;

    public GalleryController(GalleryDocumentService galleryDocumentService) {
        this.galleryDocumentService = galleryDocumentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('documentos/galeria:INCLUIR')")
    @Operation(summary = "Cria um arquivo na galeria", description = "Cria os arquivos da galeria de documentos da empresa")
    public ResponseEntity<GalleryDocumentResponseDTO> create(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") CreateGalleryDocumentDTO dto,
            Authentication authentication) {
        GalleryDocumentResponseDTO document = galleryDocumentService.create(file, dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @PreAuthorize("hasAuthority('documentos/galeria:CONSULTAR')")
    @GetMapping
    @Operation(summary = "Retorna todos os arquivos", description = "Retorna todos os arquivos da galeria")
    public ResponseEntity<List<GalleryDocumentResponseDTO>> getAll() {
        return ResponseEntity.ok(galleryDocumentService.list());
    }

    @PreAuthorize("hasAuthority('documentos/galeria:BAIXAR')")
    @GetMapping("/{id}/file")
    @Operation(summary = "Baixar arquivo", description = "Baixa o arquivo da galeria pelo ID")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID id) {
        GalleryDocument document = galleryDocumentService.findById(id);
        byte[] file = galleryDocumentService.getFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .body(file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('documentos/galeria:EXCLUIR')")
    @Operation(summary = "Deleta um arquivo", description = "Deleta o arquivo da galeria pelo ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        galleryDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
