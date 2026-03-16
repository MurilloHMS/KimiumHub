package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyReportFilterDTO;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.domain.entities.FuelSupply;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.ReportFormat;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class FuelSupplyReportService {
    private static final String REPORT_PATH = "templates/reports/fuel_supply/";

    @Autowired
    private FuelSupplyRepository repository;

    public ResponseEntity<byte[]> generateReport(FuelSupplyReportFilterDTO request) {
        try{
            LocalDate start = LocalDate.of(request.year(), request.month(), 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            List<FuelSupply> result = repository.findByFuelSupplyDateBetween(start, end);

            if(result.isEmpty()){
                return ResponseEntity.notFound().build();
            }

            List<Department> departments = result.stream()
                    .map(FuelSupply::getDepartment)
                    .distinct()
                    .sorted(Comparator.comparing(Department::name))
                    .toList();

            List<JasperPrint> prints = new ArrayList<>();
            prints.add(buildGeralPrint(result, request.month(), request.year()));

            for(Department dept : departments){
                List<FuelSupply> deptData = result.stream()
                        .filter(fs -> fs.getDepartment() == dept)
                        .sorted(Comparator
                                .comparing(FuelSupply::getPlate)
                                .thenComparing(FuelSupply::getDriverName)
                                .thenComparing(FuelSupply::getFuelSupplyDate))
                        .toList();
                prints.add(buildDepartmentPrint(deptData, dept, request.month(), request.year()));
            }

            String baseFilename = "abastecimento_" + monthName(request.month()) + "_" + request.year();

            if (request.format() == ReportFormat.PDF) {
                byte[] pdf = exportPdf(prints);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + baseFilename + ".pdf\"")
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                        .body(pdf);
            }
            byte[] xlsx = exportXlsx(prints, departments);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + baseFilename + ".xlsx\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(xlsx.length))
                    .body(xlsx);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private JasperPrint buildGeralPrint(List<FuelSupply> data, int month, int year) throws Exception {
        List<FuelSupply> sorted = data.stream()
                .sorted(Comparator
                        .comparing(FuelSupply::getUf)
                        .thenComparing(FuelSupply::getDriverName)
                        .thenComparing(FuelSupply::getPlate)
                        .thenComparing(FuelSupply::getFuelType)
                        .thenComparing(FuelSupply::getFuelSupplyDate))
                .toList();
        return fill("fuel_supply.jrxml", sorted, buildParams(data, month, year));
    }

    private JasperPrint buildDepartmentPrint(List<FuelSupply> deptData,
                                             Department dept,
                                             int mes, int ano) throws Exception {
        Map<String, Object> params = buildParams(deptData, mes, ano);
        params.put("DEPARTMENT_NAME", dept.name());
        return fill("fuel_supply_by_department.jrxml", deptData, params);
    }

    private byte[] exportPdf(List<JasperPrint> prints) throws Exception {
        if (prints.size() == 1) {
            return JasperExportManager.exportReportToPdf(prints.get(0));
        }

        JasperPrint base = prints.get(0);
        for (int i = 1; i < prints.size(); i++) {
            prints.get(i).getPages().forEach(base::addPage);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(base));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.exportReport();
        return out.toByteArray();
    }

    private byte[] exportXlsx(List<JasperPrint> prints,
                              List<Department> departments) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRXlsxExporter exporter   = new JRXlsxExporter();

        List<String> sheetNames = new ArrayList<>();
        sheetNames.add("Geral");
        departments.forEach(d -> sheetNames.add(d.name()));

        SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(true);
        config.setRemoveEmptySpaceBetweenRows(true);
        config.setDetectCellType(true);
        config.setWhitePageBackground(false);
        config.setSheetNames(sheetNames.toArray(new String[0]));

        exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.setConfiguration(config);
        exporter.exportReport();
        return out.toByteArray();
    }

    private JasperPrint fill(String jrxmlFile, List<FuelSupply> data, Map<String, Object> params) throws Exception {
        JasperReport compiled = compile(REPORT_PATH + jrxmlFile);
        return JasperFillManager.fillReport(compiled, params, new JRBeanCollectionDataSource(data));
    }

    private Map<String, Object> buildParams(List<FuelSupply> data, int mes, int ano) {
        double totalGasto  = data.stream().mapToDouble(FuelSupply::getTotalValue).sum();
        double totalLitros = data.stream().mapToDouble(FuelSupply::getLiters).sum();
        double media       = data.stream().mapToDouble(FuelSupply::getAverageKm).average().orElse(0.0);

        LocalDate refDate  = LocalDate.of(ano, mes, 1);
        LocalDate compDate = refDate.minusMonths(1);

        Map<String, Object> params = new HashMap<>();
        params.put("MES_REFERENCIA",  monthName(mes));
        params.put("ANO_REFERENCIA",  String.valueOf(ano));
        params.put("MES_COMPARATIVO", monthName(compDate.getMonthValue()));
        params.put("ANO_COMPARATIVO", String.valueOf(compDate.getYear()));
        params.put("TOTAL_GASTO",     totalGasto);
        params.put("TOTAL_LITROS",    totalLitros);
        params.put("MEDIA",           media);
        params.put("ABASTECIMENTOS",  String.valueOf(data.size()));
        return params;
    }

    private JasperReport compile(String resourcePath) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Relatório não encontrado: " + resourcePath);
        }
        return JasperCompileManager.compileReport(stream);
    }

    private String monthName(int mes) {
        return LocalDate.of(2000, mes, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
    }
}
