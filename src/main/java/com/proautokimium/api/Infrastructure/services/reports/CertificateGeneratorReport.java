package com.proautokimium.api.Infrastructure.services.reports;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class CertificateGeneratorReport {

    /**
     * Compila o template. **Esta é a parte cara.**
     *
     * Ficou separada do preenchimento por causa do lote: antes, gerar 200
     * certificados compilava o mesmo `.jrxml` 200 vezes. Agora compila uma e
     * preenche 200.
     */
    public JasperReport compile(String reportName){
        try(InputStream jasperStream = getClass().getResourceAsStream("/templates/reports/certificado/" + reportName)){

            if(jasperStream == null){
                log.error("arquivo do relatório não encontrado.");
                throw new RuntimeException("Arquivo de relatório não encontrado: " + reportName);
            }

            return JasperCompileManager.compileReport(jasperStream);
        }catch(Exception e){
            log.error("Erro ao compilar relatório: {}", e.getMessage());
            throw new RuntimeException("Erro ao compilar relatório: " + e.getMessage(), e);
        }
    }

    public byte[] fill(JasperReport jasperReport, Map<String, Object> params) {
        try{
            JasperPrint print = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource(1));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, outputStream);
            return outputStream.toByteArray();

        }catch (Exception e){
            log.error("Erro ao gerar relatório: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
        }
    }

    public byte[] generate(Map<String, Object> params, String reportName){
        return fill(compile(reportName), params);
    }
}
