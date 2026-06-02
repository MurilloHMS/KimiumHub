package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.factories.ReportFactory;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractExcelWriterService;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractService;
import com.proautokimium.api.Infrastructure.services.reports.machine.MachineContractReportService;
import com.proautokimium.api.Application.DTOs.machine.MatrizDTO;
import com.proautokimium.api.Application.DTOs.machine.ReciboLocacaoDTO;
import com.proautokimium.api.domain.models.MachineContract;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("api/machine/contract")
public class MachineContractController {

    private static final String REPORT_BASE = "/templates/reports/machine-contract/";
    private static final DecimalFormat BRL_FORMAT =
            new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));

    @Autowired
    private MachineContractService machineContractService;

    @Autowired
    private MachineContractReportService machineContractReportService;

    @Autowired
    private MachineContractExcelWriterService machineContractExcelWriterService;

    @Autowired
    private ReportFactory reportFactory;

    /**
     * Recebe a planilha de locações, gera um PDF de recibo por grupo/matriz
     * e retorna um arquivo ZIP com todos os PDFs.
     */
    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadSpreadSheet(
            @RequestPart("spreadsheet") MultipartFile file,
            @RequestPart("data") String data) throws Exception {

        List<MachineContract> contracts =
                machineContractService.getDataByExcel(file.getInputStream());

        LocalDate dataVencimento = LocalDate.parse(data, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        ReciboLocacaoDTO dto =
                machineContractReportService.build(contracts, dataVencimento);

        JasperReport subreportCompilado;
        try (InputStream is = getClass().getResourceAsStream(
                REPORT_BASE + "subreport-maquinas.jrxml")) {
            subreportCompilado = JasperCompileManager.compileReport(is);
        }

        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(zipBuffer)) {
            for (MatrizDTO matriz : dto.getMatrizes()) {

                Map<String, Object> params = buildParams(dto, matriz, subreportCompilado);

                JRBeanCollectionDataSource ds =
                        new JRBeanCollectionDataSource(matriz.getUnidades());

                byte[] pdf = reportFactory.generatePdf(
                        params, ds, "machine-contract/machine-contract-v2.jrxml");

                String fileName = sanitizeFileName(matriz.getNomeMatriz()) + ".pdf";
                zip.putNextEntry(new ZipEntry(fileName));
                zip.write(pdf);
                zip.closeEntry();
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recibos-locacao-" + dto.getMesReferencia() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBuffer.toByteArray());
    }

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildParams(
            ReciboLocacaoDTO dto,
            MatrizDTO matriz,
            JasperReport subreportCompilado) {

        InputStream backgroundImage = getClass()
                .getResourceAsStream("/templates/images/logo-proauto.png");

        Map<String, Object> params = new HashMap<>();
        params.put("P_MES_REFERENCIA",  dto.getMesReferencia());
        params.put("P_VENCIMENTO",      dto.getVencimento());
        params.put("P_DATA_EMISSAO",    dto.getDataEmissao());
        params.put("P_NOME_MATRIZ",     matriz.getNomeMatriz());
        params.put("P_TOTAL_MATRIZ",    BRL_FORMAT.format(matriz.getTotalMatriz()));
        params.put("P_TOTAL_GERAL",     BRL_FORMAT.format(dto.getTotalGeral()));
        params.put("BACKGROUND_IMAGE", backgroundImage);

        params.put("SUBREPORT_MAQUINAS", subreportCompilado);

        return params;
    }

    /**
     * Remove acentos e caracteres especiais para nome de arquivo seguro.
     * Ex: "MENU GRUPO" → "MENU_GRUPO"
     */
    private String sanitizeFileName(String nome) {
        if (nome == null) return "relatorio";
        String normalized = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                .replaceAll("_+", "_")
                .trim();
        return normalized.isEmpty() ? "relatorio" : normalized;
    }
}