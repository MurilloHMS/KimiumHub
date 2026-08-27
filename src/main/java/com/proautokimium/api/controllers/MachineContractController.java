package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.machine.*;
import com.proautokimium.api.Infrastructure.factories.ReportFactory;
import com.proautokimium.api.Infrastructure.repositories.RentalReceiptBatchRepository;
import com.proautokimium.api.Infrastructure.repositories.RentalReceiptRepository;
import com.proautokimium.api.Infrastructure.services.machine.ContractCacheService;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractExcelWriterService;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractService;
import com.proautokimium.api.Infrastructure.services.reports.machine.MachineContractReportService;
import com.proautokimium.api.Infrastructure.services.storage.RentalReceiptStorageService;
import com.proautokimium.api.domain.entities.RentalReceipt;
import com.proautokimium.api.domain.entities.RentalReceiptBatch;
import com.proautokimium.api.domain.enums.ReceiptType;
import com.proautokimium.api.domain.models.MachineContract;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("api/machine/contract")
public class MachineContractController {

    private static final String REPORT_BASE = "/templates/reports/machine-contract/";
    private static final DecimalFormat BRL_FORMAT =
            new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));

    @Autowired private MachineContractService       machineContractService;
    @Autowired private MachineContractReportService machineContractReportService;
    @Autowired private ContractCacheService         contractCacheService;
    @Autowired private ReportFactory                reportFactory;
    @Autowired private MachineContractExcelWriterService machineContractExcelWriterService;
    @Autowired private RentalReceiptStorageService  rentalReceiptStorageService;
    @Autowired private RentalReceiptBatchRepository batchRepository;
    @Autowired private RentalReceiptRepository      receiptRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // PASSO 1  →  POST /preview
    // Faz upload da planilha, armazena no cache e retorna a lista de matrizes.
    // O cliente usa essa lista para montar a tela onde informa o vencimento
    // de cada matriz antes de solicitar a geração dos PDFs.
    //
    // Request  : multipart/form-data  { spreadsheet: File }
    //
    // Response :
    // {
    //   "processId": "uuid-valido-por-30min",
    //   "matrizes": [
    //     { "codMatriz": "001", "nomeMatriz": "MENU ALIMENTAÇÃO",
    //       "totalUnidades": 3, "totalMaquinas": 4, "totalMatriz": 6319.20 }
    //   ]
    // }
    // ─────────────────────────────────────────────────────────────────────────
    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:CONSULTAR')")
    @PostMapping("/preview")
    public ResponseEntity<ReportPreviewDTO> preview(
            @RequestPart("spreadsheet") MultipartFile file) throws Exception {

        List<MachineContract> contracts =
                machineContractService.getDataByExcel(file.getInputStream());

        String processId = contractCacheService.store(contracts);

        List<MatrizPreviewDTO> matrizes =
                machineContractReportService.buildPreview(contracts);

        return ResponseEntity.ok(new ReportPreviewDTO(processId, matrizes));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASSO 2  →  POST /generate
    // Recebe processId + mapa de vencimentos, gera um PDF por matriz e
    // retorna o arquivo ZIP.
    //
    // Request  : application/json
    // {
    //   "processId":     "uuid-retornado-pelo-preview",
    //   "mesReferencia": "Maio",
    //   "vencimentos": {
    //     "001": "25/06/2026",
    //     "002": "30/06/2026"
    //   }
    // }
    //
    // Response : application/zip  →  recibos-locacao-Maio.zip
    // ─────────────────────────────────────────────────────────────────────────
    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:INCLUIR')")
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(
            @RequestBody GenerateReportRequestDTO request) throws Exception {

        List<MachineContract> contracts =
                contractCacheService.get(request.getProcessId());

        ReciboLocacaoDTO dto = machineContractReportService.build(
                contracts,
                request.getMesReferencia(),
                request.getVencimentos()
        );

        // Compila o sub-relatório uma vez, reutilizado em todos os PDFs
        JasperReport subreportCompilado;
        try (InputStream is = getClass().getResourceAsStream(
                REPORT_BASE + "subreport-maquinas.jasper")) {
            subreportCompilado = (JasperReport) JRLoader.loadObject(is);// compilar -> JasperCompileManager.compileReport(is)
        }

        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(zipBuffer)) {
            for (MatrizDTO matriz : dto.getMatrizes()) {

                byte[] pdf = reportFactory.generatePdf(
                        buildParams(dto, matriz, subreportCompilado),
                        new JRBeanCollectionDataSource(matriz.getUnidades()),
                        "machine-contract/machine-contract-v2.jasper"
                );

                zip.putNextEntry(new ZipEntry(sanitizeFileName(matriz.getNomeMatriz()) + ".pdf"));
                zip.write(pdf);
                zip.closeEntry();
            }
        }

        contractCacheService.evict(request.getProcessId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recibos-locacao-" + dto.getMesReferencia() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBuffer.toByteArray());
    }

    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:BAIXAR')")
    @GetMapping("/spreadsheet/model")
    public ResponseEntity<byte[]> getSpreadSheetModel() throws Exception {
        byte[] file = machineContractExcelWriterService.writeTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"planilha-modelo.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // V2 — Generate with persistence, exclusion, mode, name overrides
    // ─────────────────────────────────────────────────────────────────────────
    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:INCLUIR')")
    @PostMapping("/generate/v2")
    public ResponseEntity<byte[]> generateV2(
            @RequestBody GenerateReportRequestV2DTO request) throws Exception {

        List<MachineContract> allContracts =
                contractCacheService.get(request.getProcessId());

        Set<String> excluded = request.getExcludedKeys() != null
                ? new HashSet<>(request.getExcludedKeys()) : Collections.emptySet();
        Map<String, String> nomeOverrides = request.getNomeOverrides() != null
                ? request.getNomeOverrides() : Collections.emptyMap();
        Map<String, String> vencimentos = request.getVencimentos() != null
                ? request.getVencimentos() : Collections.emptyMap();
        boolean perUnit = "UNIDADE".equalsIgnoreCase(request.getMode());

        List<MachineContract> filtered;
        if (perUnit) {
            filtered = allContracts.stream()
                    .filter(c -> !excluded.contains(c.getNumeroNota()))
                    .collect(Collectors.toList());
        } else {
            filtered = allContracts.stream()
                    .filter(c -> !excluded.contains(c.getCodigoMatriz()))
                    .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ReciboLocacaoDTO dto = machineContractReportService.build(
                filtered, request.getMesReferencia(), vencimentos);

        JasperReport subreportCompilado;
        try (InputStream is = getClass().getResourceAsStream(
                REPORT_BASE + "subreport-maquinas.jasper")) {
            subreportCompilado = (JasperReport) JRLoader.loadObject(is);
        }

        RentalReceiptBatch batch = new RentalReceiptBatch();
        batch.setReferenceMonth(request.getMesReferencia());
        batch.setReferenceYear(request.getAnoReferencia());
        batch.setGeneratedAt(LocalDateTime.now());
        batch.setTotalAmount(dto.getTotalGeral());

        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(zipBuffer)) {
            for (MatrizDTO matriz : dto.getMatrizes()) {

                String matrizName = nomeOverrides.getOrDefault(
                        matriz.getCodMatriz(), matriz.getNomeMatriz());

                if (perUnit) {
                    for (UnidadeDTO unidade : matriz.getUnidades()) {
                        String unitName = nomeOverrides.getOrDefault(
                                unidade.getNumnota(), unidade.getNomeparc());
                        String venc = vencimentos.getOrDefault(
                                unidade.getNumnota(),
                                vencimentos.getOrDefault(matriz.getCodMatriz(), "Nao informado"));

                        MatrizDTO singleMatriz = new MatrizDTO();
                        singleMatriz.setCodMatriz(matriz.getCodMatriz());
                        singleMatriz.setNomeMatriz(matrizName);
                        singleMatriz.setTotalMatriz(unidade.getVlrDesdob());
                        singleMatriz.setVencimento(venc);
                        singleMatriz.setUnidades(List.of(unidade));

                        ReciboLocacaoDTO singleDto = new ReciboLocacaoDTO();
                        singleDto.setMesReferencia(dto.getMesReferencia());
                        singleDto.setDataEmissao(dto.getDataEmissao());
                        singleDto.setTotalGeral(unidade.getVlrDesdob());
                        singleDto.setMatrizes(List.of(singleMatriz));

                        byte[] pdf = reportFactory.generatePdf(
                                buildParams(singleDto, singleMatriz, subreportCompilado),
                                new JRBeanCollectionDataSource(List.of(unidade)),
                                "machine-contract/machine-contract-v2.jasper");

                        String sanitizedMatriz = sanitizeFileName(matrizName);
                        String sanitizedUnit = sanitizeFileName(unitName);
                        String zipEntryName = sanitizedMatriz + "/" + sanitizedUnit + ".pdf";
                        zip.putNextEntry(new ZipEntry(zipEntryName));
                        zip.write(pdf);
                        zip.closeEntry();

                        String storagePath = rentalReceiptStorageService.save(
                                pdf, request.getAnoReferencia(),
                                request.getMesReferencia(),
                                matriz.getCodMatriz(), sanitizedUnit);

                        RentalReceipt receipt = new RentalReceipt();
                        receipt.setBatch(batch);
                        receipt.setReceiptType(ReceiptType.UNIDADE);
                        receipt.setCodMatriz(matriz.getCodMatriz());
                        receipt.setNomeMatriz(matrizName);
                        receipt.setNumNota(unidade.getNumnota());
                        receipt.setNomeParceiro(unitName);
                        receipt.setDueDate(parseDate(venc));
                        receipt.setTotalAmount(unidade.getVlrDesdob());
                        receipt.setTotalMaquinas(unidade.getQuantidadeMaquinas());
                        receipt.setStoragePath(storagePath);
                        receipt.setOriginalFilename(sanitizedUnit + ".pdf");
                        receipt.setCreatedAt(LocalDateTime.now());
                        batch.getReceipts().add(receipt);
                    }
                } else {
                    byte[] pdf = reportFactory.generatePdf(
                            buildParams(dto, matriz, subreportCompilado),
                            new JRBeanCollectionDataSource(matriz.getUnidades()),
                            "machine-contract/machine-contract-v2.jasper");

                    String sanitized = sanitizeFileName(matrizName);
                    zip.putNextEntry(new ZipEntry(sanitized + ".pdf"));
                    zip.write(pdf);
                    zip.closeEntry();

                    String storagePath = rentalReceiptStorageService.save(
                            pdf, request.getAnoReferencia(),
                            request.getMesReferencia(),
                            matriz.getCodMatriz(), sanitized);

                    RentalReceipt receipt = new RentalReceipt();
                    receipt.setBatch(batch);
                    receipt.setReceiptType(ReceiptType.MATRIZ);
                    receipt.setCodMatriz(matriz.getCodMatriz());
                    receipt.setNomeMatriz(matrizName);
                    receipt.setDueDate(parseDate(matriz.getVencimento()));
                    receipt.setTotalAmount(matriz.getTotalMatriz());
                    receipt.setTotalUnidades(matriz.getUnidades().size());
                    receipt.setTotalMaquinas(matriz.getUnidades().stream()
                            .mapToInt(UnidadeDTO::getQuantidadeMaquinas).sum());
                    receipt.setStoragePath(storagePath);
                    receipt.setOriginalFilename(sanitized + ".pdf");
                    receipt.setCreatedAt(LocalDateTime.now());
                    batch.getReceipts().add(receipt);
                }
            }
        }

        batchRepository.save(batch);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recibos-locacao-" + request.getMesReferencia() + ".zip\"")
                .header("X-Batch-Id", batch.getId().toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBuffer.toByteArray());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History endpoints
    // ─────────────────────────────────────────────────────────────────────────
    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:CONSULTAR')")
    @GetMapping("/receipts")
    public ResponseEntity<List<ReceiptBatchSummaryDTO>> listBatches(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {

        List<RentalReceiptBatch> batches;
        if (month != null && year != null) {
            batches = batchRepository.findByReferenceMonthAndReferenceYearOrderByGeneratedAtDesc(month, year);
        } else if (year != null) {
            batches = batchRepository.findByReferenceYearOrderByGeneratedAtDesc(year);
        } else if (month != null) {
            batches = batchRepository.findByReferenceMonthOrderByGeneratedAtDesc(month);
        } else {
            batches = batchRepository.findAllByOrderByGeneratedAtDesc();
        }

        List<ReceiptBatchSummaryDTO> result = batches.stream()
                .map(b -> new ReceiptBatchSummaryDTO(
                        b.getId().toString(),
                        b.getReferenceMonth(),
                        b.getReferenceYear(),
                        b.getGeneratedAt().toString(),
                        b.getTotalAmount(),
                        b.getReceipts().size(),
                        b.getSourceFilename()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:CONSULTAR')")
    @GetMapping("/receipts/{batchId}")
    public ResponseEntity<ReceiptBatchDetailDTO> getBatchDetail(
            @PathVariable UUID batchId) {

        RentalReceiptBatch batch = batchRepository.findById(batchId)
                .orElse(null);
        if (batch == null) return ResponseEntity.notFound().build();

        ReceiptBatchSummaryDTO summary = new ReceiptBatchSummaryDTO(
                batch.getId().toString(),
                batch.getReferenceMonth(),
                batch.getReferenceYear(),
                batch.getGeneratedAt().toString(),
                batch.getTotalAmount(),
                batch.getReceipts().size(),
                batch.getSourceFilename()
        );

        List<ReceiptDetailDTO> receipts = receiptRepository.findByBatchIdOrderByCreatedAt(batchId)
                .stream()
                .map(r -> new ReceiptDetailDTO(
                        r.getId().toString(),
                        r.getReceiptType().name(),
                        r.getCodMatriz(),
                        r.getNomeMatriz(),
                        r.getNomeParceiro(),
                        r.getDueDate() != null ? r.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null,
                        r.getTotalAmount(),
                        r.getOriginalFilename()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ReceiptBatchDetailDTO(summary, receipts));
    }

    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:BAIXAR')")
    @GetMapping("/receipts/{batchId}/download")
    public ResponseEntity<byte[]> downloadBatchZip(
            @PathVariable UUID batchId) throws Exception {

        RentalReceiptBatch batch = batchRepository.findById(batchId)
                .orElse(null);
        if (batch == null) return ResponseEntity.notFound().build();

        List<RentalReceipt> receipts = receiptRepository.findByBatchIdOrderByCreatedAt(batchId);

        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBuffer)) {
            for (RentalReceipt r : receipts) {
                Path filePath = rentalReceiptStorageService.resolve(r.getStoragePath());
                if (Files.exists(filePath)) {
                    zip.putNextEntry(new ZipEntry(r.getOriginalFilename()));
                    zip.write(Files.readAllBytes(filePath));
                    zip.closeEntry();
                }
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recibos-" + batch.getReferenceMonth() + "-" + batch.getReferenceYear() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBuffer.toByteArray());
    }

    @PreAuthorize("hasAuthority('finance/rent-receipt-generator:BAIXAR')")
    @GetMapping("/receipts/file/{receiptId}")
    public ResponseEntity<InputStreamResource> downloadSingleReceipt(
            @PathVariable UUID receiptId) throws Exception {

        RentalReceipt receipt = receiptRepository.findById(receiptId)
                .orElse(null);
        if (receipt == null) return ResponseEntity.notFound().build();

        Path filePath = rentalReceiptStorageService.resolve(receipt.getStoragePath());
        if (!Files.exists(filePath)) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + receipt.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(Files.newInputStream(filePath)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equals("Nao informado") || dateStr.equals("Não informado")) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildParams(
            ReciboLocacaoDTO dto,
            MatrizDTO matriz,
            JasperReport subreportCompilado) {

        InputStream backgroundImage = getClass()
                .getResourceAsStream("/templates/images/logo-proauto.png");

        Map<String, Object> params = new HashMap<>();
        params.put("P_MES_REFERENCIA",   dto.getMesReferencia());
        params.put("P_DATA_EMISSAO",     dto.getDataEmissao());
        params.put("P_VENCIMENTO",       matriz.getVencimento()); // específico por matriz
        params.put("P_NOME_MATRIZ",      matriz.getNomeMatriz());
        params.put("P_TOTAL_MATRIZ",     BRL_FORMAT.format(matriz.getTotalMatriz()));
        params.put("P_TOTAL_GERAL",      BRL_FORMAT.format(dto.getTotalGeral()));
        params.put("BACKGROUND_IMAGE", backgroundImage);
        params.put("SUBREPORT_MAQUINAS", subreportCompilado);
        return params;
    }

    private String sanitizeFileName(String nome) {
        if (nome == null) return "relatorio";
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                .replaceAll("_+", "_")
                .trim();
    }
}