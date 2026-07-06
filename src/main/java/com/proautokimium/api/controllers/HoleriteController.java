package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.holerite.HoleriteResponseDTO;
import com.proautokimium.api.Application.DTOs.holerite.VincularHoleriteResultDTO;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.enums.HoleriteTipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public HoleriteController(HoleriteService service) {
        this.service = service;
    }

    /**
     * RH: separa o PDF e vincula cada holerite ao funcionário (por CPF).
     * competencia no formato "yyyy-MM"; tipo é ADIANTAMENTO (dia 20) ou SALARIO (dia 05).
     * @param file Documento Holerite
     * @param competencia Mês referencia
     * @param tipo Tipo de holerite (SALÁRIO ou ADIANTAMENTO)
     */
    @PostMapping(value = "/vincular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

        final HoleriteTipo tipoEnum;
        try {
            tipoEnum = HoleriteTipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Tipo inválido. Use ADIANTAMENTO ou SALARIO.");
        }

        try {
            VincularHoleriteResultDTO result = service.vincular(file, comp, tipoEnum);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao processar o PDF: " + e.getMessage());
        }
    }

    /** Holerites do funcionário logado. */
    @GetMapping("/me")
    @Operation(summary = "Holerite funcionário", description = "Retorna holerites vinculados ao usuário logado")
    public ResponseEntity<List<HoleriteResponseDTO>> meus(Authentication auth) {
        return ResponseEntity.ok(service.listarDoFuncionario(auth.getName()));
    }

    /** Baixa o PDF do holerite (dono ou RH/ADMIN). */
    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Baixa holerite", description = "Download do holerite")
    public ResponseEntity<byte[]> arquivo(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<HoleriteDocumento> docOpt = service.buscar(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("RH"));

        if (!service.podeAcessar(docOpt.get(), auth.getName(), isRh)) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.lerArquivo(docOpt.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"holerite.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
