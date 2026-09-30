package com.proautokimium.api.controllers.sales;

import com.proautokimium.api.Application.DTOs.sales.ChecklistCatalogDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistDetailDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistNotesDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSubmitDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSummaryDTO;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistCatalogService;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistService;
import com.proautokimium.api.Infrastructure.services.sales.pdf.ChecklistPdfService;
import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * O checklist de vendas.
 *
 * <ul>
 *   <li>{@code vendas/checklist} — o vendedor: emite (INCLUIR), vê os seus
 *       (CONSULTAR) e baixa o comprovante (BAIXAR).</li>
 *   <li>{@code vendas/checklists} — a Controladoria: vê todos (CONSULTAR),
 *       aprova, devolve e responde pedidos de alteração (ALTERAR), baixa
 *       (BAIXAR).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {

    static final String REVIEW = "vendas/checklists";

    private final ChecklistService service;
    private final ChecklistCatalogService catalogService;
    private final ChecklistPdfService pdfService;

    public ChecklistController(ChecklistService service, ChecklistCatalogService catalogService,
                               ChecklistPdfService pdfService) {
        this.service = service;
        this.catalogService = catalogService;
        this.pdfService = pdfService;
    }

    /**
     * O catálogo do celular. Com o ETag, o aparelho pergunta "mudou?" e recebe
     * 304 sem corpo quando não mudou — são uns 3 MB que não precisam viajar.
     *
     * <p>O ETag é FRACO ({@code W/"..."}): o Tomcat não comprime resposta com
     * ETag forte, porque o forte promete os mesmos bytes e o gzip muda os bytes.
     * Com o forte, o catálogo ia com 3,3 MB em vez de ~650 KB (medido).
     */
    @GetMapping("/catalog")
    @PreAuthorize("hasAuthority('vendas/checklist:INCLUIR')")
    public ResponseEntity<ChecklistCatalogDTO> catalog(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        ChecklistCatalogDTO catalog = catalogService.catalog();
        String etag = "W/\"" + catalog.version() + "\"";
        if (ifNoneMatch != null && ifNoneMatch.replace("W/", "").equals(etag.replace("W/", ""))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.noCache()).body(catalog);
    }

    /**
     * Envia (ou reenvia) um checklist. PUT porque o id vem do celular e o mesmo
     * envio pode chegar duas vezes: a segunda devolve o que a primeira gravou.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('vendas/checklist:INCLUIR')")
    public ResponseEntity<ChecklistDetailDTO> submit(@PathVariable UUID id, @Valid @RequestBody ChecklistSubmitDTO dto,
                                                     Authentication auth) {
        try {
            return ResponseEntity.ok(service.submit(id, dto, auth.getName()));
        } catch (DataIntegrityViolationException e) {
            // Duas tentativas do mesmo envio chegaram juntas e a outra gravou primeiro.
            return ResponseEntity.ok(service.detail(id, auth.getName(), false));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('vendas/checklist:CONSULTAR')")
    public ResponseEntity<List<ChecklistSummaryDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @PostMapping("/{id}/change-request")
    @PreAuthorize("hasAuthority('vendas/checklist:INCLUIR')")
    public ResponseEntity<ChecklistDetailDTO> requestChange(@PathVariable UUID id,
                                                            @Valid @RequestBody ChecklistNotesDTO dto,
                                                            Authentication auth) {
        return ResponseEntity.ok(service.requestChange(id, dto.notes(), auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('vendas/checklists:CONSULTAR')")
    public ResponseEntity<List<ChecklistSummaryDTO>> list(@RequestParam(required = false) List<ChecklistStatus> status) {
        return ResponseEntity.ok(service.listAll(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('vendas/checklist:CONSULTAR', 'vendas/checklists:CONSULTAR')")
    public ResponseEntity<ChecklistDetailDTO> detail(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.detail(id, auth.getName(), has(auth, REVIEW + ":CONSULTAR")));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('vendas/checklists:ALTERAR')")
    public ResponseEntity<ChecklistDetailDTO> approve(@PathVariable UUID id, @Valid @RequestBody ChecklistNotesDTO dto,
                                                      Authentication auth) {
        return ResponseEntity.ok(service.approve(id, dto.notes(), auth.getName()));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('vendas/checklists:ALTERAR')")
    public ResponseEntity<ChecklistDetailDTO> returnToSeller(@PathVariable UUID id,
                                                             @Valid @RequestBody ChecklistNotesDTO dto,
                                                             Authentication auth) {
        return ResponseEntity.ok(service.returnToSeller(id, dto.notes(), auth.getName()));
    }

    @PostMapping("/{id}/grant-change")
    @PreAuthorize("hasAuthority('vendas/checklists:ALTERAR')")
    public ResponseEntity<ChecklistDetailDTO> grantChange(@PathVariable UUID id,
                                                          @Valid @RequestBody ChecklistNotesDTO dto,
                                                          Authentication auth) {
        return ResponseEntity.ok(service.grantChange(id, dto.notes(), auth.getName()));
    }

    @PostMapping("/{id}/deny-change")
    @PreAuthorize("hasAuthority('vendas/checklists:ALTERAR')")
    public ResponseEntity<ChecklistDetailDTO> denyChange(@PathVariable UUID id,
                                                         @Valid @RequestBody ChecklistNotesDTO dto,
                                                         Authentication auth) {
        return ResponseEntity.ok(service.denyChange(id, dto.notes(), auth.getName()));
    }

    /** O comprovante. O vendedor baixa o seu; a Controladoria, qualquer um. */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyAuthority('vendas/checklist:BAIXAR', 'vendas/checklists:BAIXAR')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id, Authentication auth) {
        Checklist checklist = service.findVisible(id, auth.getName(), has(auth, REVIEW + ":BAIXAR"));
        byte[] pdf = pdfService.generate(checklist);
        String number = checklist.getNumber() == null ? "checklist" : String.format("%04d", checklist.getNumber());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("checklist-" + number + ".pdf").build().toString())
                .body(pdf);
    }

    private static boolean has(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
