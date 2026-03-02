package com.proautokimium.api.Infrastructure.services.reports;

import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@Service
public class EmailSignatureService {

    public byte[] generate(Map<String, Object> params, String reportName){
        try{
            InputStream jasperStream  = getClass().getResourceAsStream("/templates/reports/assinatura_email/" + reportName );
            if(jasperStream == null){
                throw new FileNotFoundException("Arquivo de relatório não encontrado: " + reportName);
            }

            JasperReport jr = JasperCompileManager.compileReport(jasperStream);

            JasperPrint print = JasperFillManager.fillReport(jr, params, new JREmptyDataSource(1));

            BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(print, 0,2f);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }catch(Exception e){
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }
}
