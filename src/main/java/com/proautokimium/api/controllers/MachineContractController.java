package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.machine.*;
import com.proautokimium.api.Infrastructure.factories.ReportFactory;
import com.proautokimium.api.Infrastructure.services.machine.ContractCacheService;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractExcelWriterService;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractService;
import com.proautokimium.api.Infrastructure.services.reports.machine.MachineContractReportService;
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

    @Autowired private MachineContractService       machineContractService;
    @Autowired private MachineContractReportService machineContractReportService;
    @Autowired private ContractCacheService         contractCacheService;
    @Autowired private ReportFactory                reportFactory;
    @Autowired private MachineContractExcelWriterService machineContractExcelWriterService;

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
            subreportCompilado = JasperCompileManager.compileReport(is);
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
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

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