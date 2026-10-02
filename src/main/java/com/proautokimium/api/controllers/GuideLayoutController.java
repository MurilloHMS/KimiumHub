package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.guide.GuideLayoutCatalogDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutDraftRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutImageDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutPreviewRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutPublishRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutStateDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutVersionDTO;
import com.proautokimium.api.Infrastructure.services.reports.guide.GuideLayoutService;
import com.proautokimium.api.Infrastructure.services.reports.guide.GuideReportService;
import com.proautokimium.api.domain.entities.guide.GuideLayoutImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * O layout do Guia de Utilização, editado pelo Design na tela.
 *
 * Quem edita tem {@code company/guide:CONFIGURAR}; quem só gera guia
 * (Contratos) tem {@code :INCLUIR} e nunca chega aqui para escrever.
 *
 * As leituras aceitam as duas: Contratos não precisa do layout para gerar (a
 * API usa o publicado), mas a tela mostra qual versão está em uso.
 */
@RestController
@RequestMapping("/api/v1/reports/guide/layout")
@Tag(name = "Relatórios", description = "Geração de relatórios e guias")
public class GuideLayoutController {

    private static final String LER =
            "hasAnyAuthority('company/guide:CONSULTAR', 'company/guide:INCLUIR', 'company/guide:CONFIGURAR')";
    private static final String EDITAR = "hasAuthority('company/guide:CONFIGURAR')";

    private final GuideLayoutService layoutService;
    private final GuideReportService reportService;

    public GuideLayoutController(GuideLayoutService layoutService, GuideReportService reportService) {
        this.layoutService = layoutService;
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize(LER)
    @Operation(summary = "O layout publicado e o rascunho, se houver")
    public ResponseEntity<GuideLayoutStateDTO> state() {
        return ResponseEntity.ok(layoutService.state());
    }

    @GetMapping("/catalog")
    @PreAuthorize(LER)
    @Operation(summary = "Campos da célula, imagens e fontes que o editor pode oferecer")
    public ResponseEntity<GuideLayoutCatalogDTO> catalog() {
        return ResponseEntity.ok(layoutService.catalog());
    }

    @PutMapping("/draft")
    @PreAuthorize(EDITAR)
    @Operation(summary = "Salva o rascunho. Não muda o guia de ninguém até publicar")
    public ResponseEntity<GuideLayoutDTO> saveDraft(Authentication auth, @RequestBody @Valid GuideLayoutDraftRequestDTO dto) {
        return ResponseEntity.ok(layoutService.saveDraft(dto.document(), auth.getName()));
    }

    @DeleteMapping("/draft")
    @PreAuthorize(EDITAR)
    @Operation(summary = "Descarta o rascunho")
    public ResponseEntity<Void> discardDraft() {
        layoutService.discardDraft();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/publish")
    @PreAuthorize(EDITAR)
    @Operation(summary = "Publica o rascunho: a partir daqui todo guia gerado usa este layout")
    public ResponseEntity<GuideLayoutDTO> publish(Authentication auth, @RequestBody @Valid GuideLayoutPublishRequestDTO dto) {
        return ResponseEntity.ok(layoutService.publish(dto.note(), auth.getName()));
    }

    @GetMapping("/versions")
    @PreAuthorize(EDITAR)
    @Operation(summary = "Versões publicadas, da mais nova para a mais antiga")
    public ResponseEntity<List<GuideLayoutVersionDTO>> versions() {
        return ResponseEntity.ok(layoutService.versions());
    }

    @PostMapping("/versions/{id}/restore")
    @PreAuthorize(EDITAR)
    @Operation(summary = "Abre uma versão antiga como rascunho. Não publica")
    public ResponseEntity<GuideLayoutDTO> restore(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(layoutService.restore(id, auth.getName()));
    }

    /** A prévia do designer. Não grava nada, por isso não é INCLUIR. */
    @PostMapping(value = "/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(EDITAR)
    @Operation(summary = "Gera o PDF do layout que está na tela, com produtos de exemplo")
    public ResponseEntity<byte[]> preview(@RequestBody @Valid GuideLayoutPreviewRequestDTO dto) {
        byte[] pdf = reportService.preview(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"previa-do-guia.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(EDITAR)
    @Operation(summary = "Envia uma imagem para o cabeçalho ou o rodapé (PNG ou JPG, até 2 MB)")
    public ResponseEntity<GuideLayoutImageDTO> uploadImage(Authentication auth, @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(layoutService.saveImage(file, auth.getName()));
    }

    /** Imagem não muda depois de enviada: o id é do conteúdo, então o cache pode ser longo. */
    @GetMapping("/images/{id}")
    @PreAuthorize(LER)
    @Operation(summary = "Baixa uma imagem enviada pelo designer")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        GuideLayoutImage image = layoutService.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(image.getContent());
    }
}
