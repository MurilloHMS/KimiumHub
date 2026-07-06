package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.guide.GuideReportRequestDTO;
import com.proautokimium.api.Infrastructure.services.reports.guide.GuideReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Controller responsável pela geração do "Guia de Utilização" em PDF.
 *
 * <p>
 * O corpo da requisição é {@code multipart/form-data} com duas partes:
 * <ul>
 *   <li>{@code request} — JSON com {@link GuideReportRequestDTO}
 *       (título do guia + lista de IDs dos produtos selecionados)</li>
 *   <li>{@code logoCliente} — arquivo de imagem do logo do cliente</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios", description = "Geração de relatórios e guias")
public class GuideReportController {

    private final GuideReportService guideReportService;

    public GuideReportController(GuideReportService guideReportService) {
        this.guideReportService = guideReportService;
    }

    /**
     * Gera o Guia de Utilização com os produtos selecionados.
     *
     * @param request     JSON com título e IDs dos produtos na ordem desejada
     * @param logoCliente Logo do cliente (PNG ou JPG)
     * @return PDF para download
     */
    @PostMapping(
            value    = "/guide",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary     = "Gerar Guia de Utilização",
            description = "Gera o PDF do Guia de Utilização com os produtos selecionados, na ordem enviada."
    )
    public ResponseEntity<byte[]> gerarGuia(
            @Parameter(description = "JSON com tituloGuia e productIds")
            @RequestPart("request")
            @Valid GuideReportRequestDTO request,

            @Parameter(description = "Logo do cliente (PNG ou JPG)")
            @RequestPart("logoCliente")
            MultipartFile logoCliente
    ) throws IOException {

        InputStream logoStream = logoCliente.isEmpty() ? null : logoCliente.getInputStream();
        byte[] pdf = guideReportService.gerarGuia(request, logoStream);

        String filename = "guia-" + request.tituloGuia()
                .toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9\\-]", "")
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}