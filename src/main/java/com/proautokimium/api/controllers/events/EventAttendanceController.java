package com.proautokimium.api.controllers.events;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendanceDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AudienceOptionsDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationAnswerDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.RespondRequestDTO;
import com.proautokimium.api.Infrastructure.services.events.EventAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A confirmação de presença.
 *
 * <p><b>Ser convidado basta.</b> Os endpoints de quem responde não pedem
 * permissão de tela: quem decide é o serviço, pelo público do evento, e quem
 * não é convidado leva 404. O {@code anyRequest()} da segurança já exige login e
 * barra o cliente da Área do Cliente.
 *
 * <p>O Acompanhamento é do cadastro ({@code communication/events:CONSULTAR}):
 * mostra nome, resposta e observação de outras pessoas.
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos — presença", description = "Convites, respostas, visualizações e o Acompanhamento")
public class EventAttendanceController {

    private static final String MANAGE = CompanyEventController.MANAGE;

    private final EventAttendanceService service;

    public EventAttendanceController(EventAttendanceService service) {
        this.service = service;
    }

    @GetMapping("/invitations")
    @Operation(summary = "Meus convites")
    public ResponseEntity<List<InvitationDTO>> myInvitations(Authentication authentication) {
        return ResponseEntity.ok(service.myInvitations(authentication.getName()));
    }

    @GetMapping("/invitations/{id}")
    @Operation(summary = "Um convite", description = "O evento com a programação e a minha resposta; 404 para quem não foi convidado")
    public ResponseEntity<InvitationDetailDTO> invitation(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.invitation(id, authentication.getName()));
    }

    @PostMapping("/{id}/response")
    @Operation(summary = "Responder", description = "Vou / Não vou, com observação; pode mudar até o evento começar")
    public ResponseEntity<InvitationAnswerDTO> respond(@PathVariable UUID id,
                                                       @Valid @RequestBody RespondRequestDTO data,
                                                       Authentication authentication) {
        return ResponseEntity.ok(service.respond(id, authentication.getName(), data.answer(), data.note()));
    }

    @PostMapping("/{id}/views")
    @Operation(summary = "Registrar que abri o evento")
    public ResponseEntity<Void> registerView(@PathVariable UUID id, Authentication authentication) {
        service.registerView(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAuthority('" + MANAGE + ":CONSULTAR')")
    @Operation(summary = "Acompanhamento", description = "Quem viu, quem respondeu, as observações e os lembretes")
    public ResponseEntity<AttendanceDTO> attendance(@PathVariable UUID id) {
        return ResponseEntity.ok(service.attendance(id));
    }

    /** Para os seletores do formulário: quem cria ou edita evento, sem precisar do RH. */
    @GetMapping("/audience-options")
    @PreAuthorize("hasAnyAuthority('" + MANAGE + ":INCLUIR', '" + MANAGE + ":ALTERAR')")
    public ResponseEntity<AudienceOptionsDTO> audienceOptions() {
        return ResponseEntity.ok(service.audienceOptions());
    }
}
