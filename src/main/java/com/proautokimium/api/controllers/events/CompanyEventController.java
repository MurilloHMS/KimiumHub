package com.proautokimium.api.controllers.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventRequestDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventSummaryDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.TalkRequestDTO;
import com.proautokimium.api.Infrastructure.services.events.CompanyEventService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Eventos da empresa.
 *
 * <p><b>Duas telas, duas permissões</b> (V106):
 * <ul>
 *   <li>{@code documentos/eventos} — ver os publicados;</li>
 *   <li>{@code communication/events} — cadastrar: {@code CONSULTAR} abre a lista
 *       com rascunhos, {@code INCLUIR} cria, {@code ALTERAR} edita, publica e mexe
 *       na programação, {@code EXCLUIR} apaga.</li>
 * </ul>
 *
 * <p>A programação é {@code ALTERAR} do evento, e não {@code INCLUIR}/{@code EXCLUIR}
 * próprios: acrescentar uma palestra é editar o evento, e quem pode editar o
 * evento e não a grade dele seria uma permissão que ninguém consegue explicar.
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos", description = "Eventos da empresa, programação e publicação")
public class CompanyEventController {

    static final String VIEW = "documentos/eventos";
    static final String MANAGE = "communication/events";

    private static final String LER =
            "hasAnyAuthority('" + VIEW + ":CONSULTAR', '" + MANAGE + ":CONSULTAR')";

    private final CompanyEventService service;

    public CompanyEventController(CompanyEventService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(LER)
    @Operation(summary = "Eventos publicados", description = "Os cards de Documentos → Eventos")
    public ResponseEntity<List<EventSummaryDTO>> listPublished() {
        return ResponseEntity.ok(service.listPublished());
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('" + MANAGE + ":CONSULTAR')")
    @Operation(summary = "Todos os eventos", description = "O cadastro, com rascunhos")
    public ResponseEntity<List<EventSummaryDTO>> listForManagement() {
        return ResponseEntity.ok(service.listForManagement());
    }

    /**
     * O mesmo endpoint serve Documentos e o "Ver como fica" do cadastro; o que
     * muda é se o rascunho abre. Decidido pela authority de quem pede, e nunca
     * por parâmetro da URL.
     */
    @GetMapping("/{id}")
    @PreAuthorize(LER)
    public ResponseEntity<EventDetailDTO> get(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.get(id, can(authentication, MANAGE + ":CONSULTAR")));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + MANAGE + ":INCLUIR')")
    public ResponseEntity<EventDetailDTO> create(
            @Valid @RequestPart("data") EventRequestDTO data,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(service.create(data, cover, authentication.getName()));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> update(
            @PathVariable UUID id,
            @Valid @RequestPart("data") EventRequestDTO data,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(service.update(id, data, cover, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + MANAGE + ":EXCLUIR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) throws IOException {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> publish(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.publish(id, authentication.getName()));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> unpublish(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.unpublish(id, authentication.getName()));
    }

    // ─── Programação ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/talks")
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> addTalk(@PathVariable UUID id,
                                                  @Valid @RequestBody TalkRequestDTO data,
                                                  Authentication authentication) {
        return ResponseEntity.ok(service.addTalk(id, data, authentication.getName()));
    }

    @PutMapping("/{id}/talks/{talkId}")
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> updateTalk(@PathVariable UUID id,
                                                     @PathVariable UUID talkId,
                                                     @Valid @RequestBody TalkRequestDTO data,
                                                     Authentication authentication) {
        return ResponseEntity.ok(service.updateTalk(id, talkId, data, authentication.getName()));
    }

    @DeleteMapping("/{id}/talks/{talkId}")
    @PreAuthorize("hasAuthority('" + MANAGE + ":ALTERAR')")
    public ResponseEntity<EventDetailDTO> deleteTalk(@PathVariable UUID id,
                                                     @PathVariable UUID talkId,
                                                     Authentication authentication) {
        return ResponseEntity.ok(service.deleteTalk(id, talkId, authentication.getName()));
    }

    private static boolean can(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
