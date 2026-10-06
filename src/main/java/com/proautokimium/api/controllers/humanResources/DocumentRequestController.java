package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AudienceOptionsDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.CreateDocumentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.DocumentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.RecipientDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.RequestFileDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.ReturnAnswerDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.SendDocumentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.SubmitAnswersDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.UpdateDocumentRequestDTO;
import com.proautokimium.api.Infrastructure.services.events.EventAttendanceService;
import com.proautokimium.api.Infrastructure.services.humanResources.DocumentRequestService;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Solicitações do RH: o RH pede (arquivo ou resposta), o funcionário responde,
 * o RH confere.
 *
 * Duas telas, e uma não faz o papel da outra:
 * - `rh/document-requests`: criar, editar, enviar, encerrar, acompanhar e conferir;
 * - `documentos/rh/requests`: o funcionário vê e responde as DELE.
 *
 * Nas rotas `/me`, quem responde vem SEMPRE do login (`auth.getName()`); o
 * serviço confere o dono e devolve 404 para quem não é.
 */
@RestController
@RequestMapping("/api/hr/document-requests")
@Tag(name = "Solicitações do RH", description = "Pedir documentos e respostas aos funcionários")
public class DocumentRequestController {

    private static final String HR_DOWNLOAD = "rh/document-requests:BAIXAR";

    private final DocumentRequestService service;
    private final EventAttendanceService audience;

    public DocumentRequestController(DocumentRequestService service, EventAttendanceService audience) {
        this.service = service;
        this.audience = audience;
    }

    // ── RH ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('rh/document-requests:CONSULTAR')")
    @Operation(summary = "Lista as solicitações", description = "A mais nova primeiro, com os contadores das respostas")
    public ResponseEntity<List<DocumentRequestDTO>> list() {
        return ResponseEntity.ok(service.listRequests());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/document-requests:CONSULTAR')")
    public ResponseEntity<DocumentRequestDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getRequest(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/document-requests:INCLUIR')")
    @Operation(summary = "Cria o rascunho", description = "Só com o título; campos e público vêm depois")
    public ResponseEntity<DocumentRequestDTO> create(@RequestBody CreateDocumentRequestDTO body, Authentication auth) {
        UUID id = service.createDraft(body.title(), auth.getName()).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.getRequest(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/document-requests:ALTERAR')")
    @Operation(summary = "Edita o rascunho", description = "Título, instruções, prazo e campos; recusado depois do envio")
    public ResponseEntity<DocumentRequestDTO> update(@PathVariable UUID id, @RequestBody UpdateDocumentRequestDTO body) {
        service.updateDraft(id, body.title(), body.instructions(), body.dueDate(), body.form());
        return ResponseEntity.ok(service.getRequest(id));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('rh/document-requests:ENVIAR')")
    @Operation(summary = "Envia", description = "Cria um destinatário por pessoa do público e avisa cada uma")
    public ResponseEntity<DocumentRequestDTO> send(@PathVariable UUID id, @RequestBody SendDocumentRequestDTO body) {
        service.send(id, body.all(), orEmpty(body.companyIds()), orEmpty(body.departmentIds()), orEmpty(body.employeeIds()));
        return ResponseEntity.ok(service.getRequest(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('rh/document-requests:ALTERAR')")
    @Operation(summary = "Encerra", description = "Não aceita mais respostas; as já enviadas continuam conferíveis")
    public ResponseEntity<DocumentRequestDTO> close(@PathVariable UUID id) {
        service.close(id);
        return ResponseEntity.ok(service.getRequest(id));
    }

    /** As opções do seletor de público: as mesmas dos Eventos, com quem pode receber. */
    @GetMapping("/audience-options")
    @PreAuthorize("hasAuthority('rh/document-requests:CONSULTAR')")
    public ResponseEntity<AudienceOptionsDTO> audienceOptions() {
        return ResponseEntity.ok(audience.audienceOptions());
    }

    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAuthority('rh/document-requests:CONSULTAR')")
    @Operation(summary = "Respostas de uma solicitação", description = "Cada pessoa, com status, respostas e arquivos atuais")
    public ResponseEntity<List<RecipientDTO>> recipients(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listRecipients(id));
    }

    /** A aba Solicitações da Pendências: o que espera conferência, o mais antigo primeiro. */
    @GetMapping("/recipients/awaiting-review")
    @PreAuthorize("hasAuthority('rh/document-requests:CONSULTAR')")
    public ResponseEntity<List<RecipientDTO>> awaitingReview() {
        return ResponseEntity.ok(service.listAwaitingReview());
    }

    @PostMapping("/recipients/{recipientId}/approve")
    @PreAuthorize("hasAuthority('rh/document-requests:ALTERAR')")
    @Operation(summary = "Aprova a resposta", description = "Arquivos de campo com tipo viram documento do funcionário")
    public ResponseEntity<RecipientDTO> approve(@PathVariable UUID recipientId, Authentication auth) {
        service.approve(recipientId, auth.getName());
        return ResponseEntity.ok(service.getRecipient(recipientId));
    }

    @PostMapping("/recipients/{recipientId}/return")
    @PreAuthorize("hasAuthority('rh/document-requests:ALTERAR')")
    @Operation(summary = "Devolve a resposta", description = "Com motivo obrigatório; o funcionário corrige e reenvia")
    public ResponseEntity<RecipientDTO> giveBack(@PathVariable UUID recipientId, @RequestBody ReturnAnswerDTO body,
                                                 Authentication auth) {
        service.giveBack(recipientId, auth.getName(), body.reason());
        return ResponseEntity.ok(service.getRecipient(recipientId));
    }

    // ── Funcionário ─────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('documentos/rh/requests:CONSULTAR')")
    @Operation(summary = "Minhas solicitações", description = "As do funcionário autenticado, a mais nova primeiro")
    public ResponseEntity<List<RecipientDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @PostMapping(value = "/me/{recipientId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('documentos/rh/requests:INCLUIR')")
    @Operation(summary = "Anexa um arquivo", description = "Um por campo; reenviar substitui o anterior. 10 MB, PDF, JPG ou PNG")
    public ResponseEntity<RequestFileDTO> upload(@PathVariable UUID recipientId,
                                                 @RequestParam String fieldKey,
                                                 @RequestParam("file") MultipartFile file,
                                                 Authentication auth) throws IOException {
        DocumentRequestFile saved = service.upload(recipientId, auth.getName(), fieldKey, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RequestFileDTO(saved.getId(), saved.getFieldKey(), saved.getOriginalFilename(), saved.getUploadedAt()));
    }

    @PostMapping("/me/{recipientId}/submit")
    @PreAuthorize("hasAuthority('documentos/rh/requests:INCLUIR')")
    @Operation(summary = "Envia a resposta", description = "Confere os obrigatórios; os arquivos já devem ter subido")
    public ResponseEntity<RecipientDTO> submit(@PathVariable UUID recipientId, @RequestBody SubmitAnswersDTO body,
                                               Authentication auth) {
        service.submit(recipientId, auth.getName(), body.answers());
        return ResponseEntity.ok(service.getRecipient(recipientId));
    }

    // ── Os dois ─────────────────────────────────────────────────────────────

    /**
     * O arquivo de uma resposta. Os dois lados usam o mesmo endpoint: o RH baixa
     * qualquer um; o funcionário, só os dele (o serviço confere e devolve 404).
     */
    @GetMapping("/files/{fileId}")
    @PreAuthorize("hasAnyAuthority('rh/document-requests:BAIXAR', 'documentos/rh/requests:BAIXAR')")
    public ResponseEntity<byte[]> download(@PathVariable UUID fileId, Authentication auth) throws IOException {
        // equals, e não contains: "vê de todos" é ter a tela do RH, exatamente.
        boolean isReviewer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(HR_DOWNLOAD));
        DocumentRequestService.FileContent content = service.readFile(fileId, auth.getName(), isReviewer);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.filename(), StandardCharsets.UTF_8).build().toString())
                .contentType(content.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(content.contentType()))
                .body(content.bytes());
    }

    private static Set<UUID> orEmpty(Set<UUID> ids) {
        return ids == null ? Set.of() : ids;
    }
}
