package com.proautokimium.api.Infrastructure.services.reports;

import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Service
public class CertificateGeneratorReport {

    public byte[] generate(Map<String, Object> params, String reportName){
        try{
            InputStream jasperStream = getClass().getResourceAsStream("/templates/reports/certificado/" + reportName);
            if(jasperStream == null){
                throw new RuntimeException("Arquivo de relatório não encontrado: " + reportName);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jasperStream);

            JasperPrint print = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource(1));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, outputStream);
            return outputStream.toByteArray();
        }catch (Exception e){
            throw new RuntimeException("Erro ao gerar relatório", e);
        }
    }
}
