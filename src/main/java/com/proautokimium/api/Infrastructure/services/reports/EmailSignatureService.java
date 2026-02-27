package com.proautokimium.api.Infrastructure.services.reports;

import net.sf.jasperreports.engine.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class EmailSignatureService {

    public byte[] generate(Map<String, Object> params, String reportName){
        try{
            InputStream jasperStream  = getClass().getResourceAsStream("/templates/reports/assinatura_email/" + reportName );
            if(jasperStream == null){
                throw new FileNotFoundException("Arquivo de relatório não encontrado: " + reportName);
            }

            JasperReport jr = JasperCompileManager.compileReport(jasperStream);

            JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource(0));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, bos);
            return bos.toByteArray();
        }catch(Exception e){
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }
}
