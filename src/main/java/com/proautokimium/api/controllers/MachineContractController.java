package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.factories.ReportFactory;
import com.proautokimium.api.Infrastructure.services.machine.MachineContractService;
import com.proautokimium.api.Infrastructure.services.reports.machine.MachineContractReportService;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.ReciboLocacaoDTO;
import com.proautokimium.api.domain.models.MachineContract;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("api/machine/contract")
public class MachineContractController {

    @Autowired
    private MachineContractService machineContractService;

    @Autowired
    private MachineContractReportService machineContractReportService;

    @Autowired
    private ReportFactory reportFactory;

    @PostMapping("/upload")
    private ResponseEntity<Object> uploadSpreadSheet(@RequestPart("spreadsheet")MultipartFile file) throws Exception {

        List<MachineContract> contracts = machineContractService.getDataByExcel(file.getInputStream());

        ReciboLocacaoDTO dto =
                machineContractReportService.build(
                        contracts,
                        "Maio",
                        "25/06/2026"
                );

        JRBeanCollectionDataSource dataSource =
                new JRBeanCollectionDataSource(dto.getMatrizes());

        Map<String, Object> params = new HashMap<>();

        params.put("P_MES_REFERENCIA", dto.getMesReferencia());
        params.put("P_VENCIMENTO", dto.getVencimento());
        params.put("P_DATA_EMISSAO", dto.getDataEmissao());
        params.put("P_TOTAL_GERAL", dto.getTotalGeral());
        params.put( "SUBREPORT_DIR", Objects.requireNonNull(getClass().getResource("/templates/reports/machine-contract/")).toString());

        byte[] pdf = reportFactory.generatePdf(
                params,
                dataSource,
                "machine-contract/machine-contract-v2.jrxml"
        );

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=recibo-locacao.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
