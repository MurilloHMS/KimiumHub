package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.holerite.*;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs;
import com.proautokimium.api.Infrastructure.services.holerite.PayslipTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/holerite")
@Tag(name = "Holerite", description = "Vincula holerite")
public class HoleriteController {

    private final HoleriteService service;

    private final PayslipTypeService payslipTypes;

    public HoleriteController(HoleriteService service, PayslipTypeService payslipTypes) {
        this.service = service;
        this.payslipTypes = payslipTypes;
    }

    /**
     * RH: separa o PDF e vincula cada holerite ao funcionário (por CPF).
     * competencia no formato "yyyy-MM"; tipo é o código de um tipo cadastrado
     * (SALARIO, ADIANTAMENTO, PLR… — ver {@code GET /types}).
     * @param file Documento Holerite
     * @param competencia Mês referencia
     * @param tipo Código do tipo de holerite
     */
    @PostMapping(value = "/vincular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('rh/holerit:INCLUIR')")
    @Operation(summary = "Vincula Holerite", description = "Vincula um holerite ao funcionário")
    public ResponseEntity<?> vincular(@RequestParam("file") MultipartFile file,
                                      @RequestParam("competencia") String competencia,
                                      @RequestParam("tipo") String tipo) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo inválido");
        }

        final LocalDate comp;
        try {
            comp = LocalDate.parse(competencia + "-01");
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Competência inválida. Use o formato AAAA-MM.");
        }

        // O tipo é conferido no serviço, contra o cadastro: desconhecido vira
        // 400 com o código na mensagem (InvalidPayslipTypeException).
        try {
            VincularHoleriteResultDTO result = service.vincular(file, comp, tipo);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao processar o PDF: " + e.getMessage());
        }
    }

    /** Holerites do funcionário logado. */
    @PreAuthorize("hasAuthority('documentos/holerites:CONSULTAR')")
    @GetMapping("/me")
    @Operation(summary = "Holerite funcionário", description = "Retorna holerites vinculados ao usuário logado")
    public ResponseEntity<List<HoleriteResponseDTO>> meus(Authentication auth) {
        return ResponseEntity.ok(service.listarDoFuncionario(auth.getName()));
    }

    /** Baixa o PDF do holerite (dono ou RH/ADMIN). */
    @PreAuthorize("hasAnyAuthority('rh/holerit:BAIXAR', 'documentos/holerites:BAIXAR')")
    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Baixa holerite", description = "Download do holerite")
    public ResponseEntity<byte[]> arquivo(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<HoleriteDocumento> docOpt = service.buscar(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // "Vê de todos" é ter a tela do RH — a mesma authority do @PreAuthorize.
        // equals, e não contains: contains("ADMIN") casava com ROLE_ADMINISTRATIVO.
        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("rh/holerit:BAIXAR"));

        if (!service.podeAcessar(docOpt.get(), auth.getName(), isRh)) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.lerArquivo(docOpt.get());

        // Marca a primeira abertura. O serviço ignora quando quem baixa é o RH:
        // conferir um holerite não é a pessoa tê-lo visto.
        service.registrarAbertura(docOpt.get(), auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"holerite.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/vincular/preview")
    @PreAuthorize("hasAuthority('rh/holerit:CONSULTAR')")
    @Operation(summary = "Confere antes de enviar", description = "Analisa o PDF e diz o que aconteceria, sem gravar nada")
    public ResponseEntity<List<HoleritePreviewItemDTO>> preview(
            @RequestParam MultipartFile file,
            @RequestParam String competencia,
            @RequestParam String tipo) throws IOException {
        return ResponseEntity.ok(service.preview(file, LocalDate.parse(competencia + "-01"), tipo));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAuthority('rh/holerit:CONSULTAR')")
    @Operation(summary = "Auditoria dos holerites", description = "Quem recebeu, quem abriu e quem confirmou")
    public ResponseEntity<List<HoleriteAuditoriaDTO>> auditoria(
            @RequestParam String competencia,
            @RequestParam String tipo) {
        return ResponseEntity.ok(service.auditoria(LocalDate.parse(competencia + "-01"), tipo.strip().toUpperCase()));
    }

    /**
     * Os tipos de holerite. O funcionário também lê: a tela dele mostra o nome
     * ("Férias coletivas"), e não o código.
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('rh/holerit:CONSULTAR', 'documentos/holerites:CONSULTAR')")
    @Operation(summary = "Tipos de holerite", description = "Os tipos ativos, na ordem")
    public ResponseEntity<List<PayslipTypeDTOs.PayslipTypeDTO>> types() {
        return ResponseEntity.ok(payslipTypes.list());
    }

    /**
     * O RH cria um tipo no meio do envio. Nome que já existe (sem contar caixa
     * nem acento) devolve o existente com {@code created=false}.
     */
    @PostMapping("/types")
    @PreAuthorize("hasAuthority('rh/holerit:INCLUIR')")
    @Operation(summary = "Cria tipo de holerite", description = "Ou devolve o existente com o mesmo nome")
    public ResponseEntity<PayslipTypeDTOs.CreatePayslipTypeResult> createType(
            @RequestBody @jakarta.validation.Valid PayslipTypeDTOs.CreatePayslipTypeRequest dto, Authentication auth) {
        PayslipTypeDTOs.CreatePayslipTypeResult result = payslipTypes.create(dto.label(), auth.getName());
        return ResponseEntity.status(result.created() ? 201 : 200).body(result);
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('rh/holerit:EXCLUIR')")
    public ResponseEntity<Object> cancelar(@PathVariable UUID id,
                                           @RequestBody CancelarHoleriteDTO dto,
                                           Authentication auth) {
        service.cancelar(id, dto.motivo(), auth.getName());
        return ResponseEntity.ok("Holerite cancelado.");
    }

    @PutMapping(value = "/{id}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('rh/holerit:ALTERAR')")
    public ResponseEntity<Object> substituirArquivo(@PathVariable UUID id,
                                                    @RequestParam MultipartFile file,
                                                    Authentication auth) throws IOException {
        service.substituirArquivo(id, file, auth.getName());
        return ResponseEntity.ok("Arquivo substituído. O registro de visualização foi zerado.");
    }

    /** Sem @PreAuthorize de propósito: quem confirma é o dono, e o serviço recusa o resto. */
    @PreAuthorize("hasAuthority('documentos/holerites:ALTERAR')")
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Object> confirmar(@PathVariable UUID id, Authentication auth) {
        service.confirmarRecebimento(id, auth.getName());
        return ResponseEntity.ok("Recebimento confirmado.");
    }
}
