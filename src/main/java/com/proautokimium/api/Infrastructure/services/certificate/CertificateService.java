package com.proautokimium.api.Infrastructure.services.certificate;

import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.helpers.ZipHelper;
import com.proautokimium.api.Infrastructure.interfaces.certificate.CertificateGenerator;
import com.proautokimium.api.Infrastructure.services.reports.CertificateGeneratorReport;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class CertificateService implements CertificateGenerator {

    private static final String TEMPLATE = "certificate.jrxml";
    private static final String BACKGROUND = "/templates/images/certificado/certificado_padrao.png";

    @Autowired
    CertificateGeneratorReport certificateGeneratorReport;

    @Override
    public byte[] generateCertificate(String name) {
        Map<String,Object> params = new HashMap<>();
        params.put("NAME",name);
        params.put("BACKGROUND_IMAGE",new ByteArrayInputStream(loadBackground()));
        return certificateGeneratorReport.generate(params, TEMPLATE);
    }

    @Override
    public byte[] generateCertificatesZip(List<String> names){
        JasperReport template = certificateGeneratorReport.compile(TEMPLATE);
        byte[] background = loadBackground();

        Map<String, byte[]> files = new LinkedHashMap<>();

        for(String name : names){
            String clean = name.trim().toUpperCase(Locale.ROOT);

            Map<String,Object> params = new HashMap<>();
            params.put("NAME",clean);
            params.put("BACKGROUND_IMAGE",new ByteArrayInputStream(background));

            byte[] pdf = certificateGeneratorReport.fill(template, params);
            files.put(ZipHelper.uniqueName(clean, files.keySet()), pdf);
        }

        try{
            return ZipHelper.zip(files);
        } catch (IOException e) {
            throw new FailedToCreateCertificate("Falha ao montar o ZIP dos certificados", e);
        }
    }

    private byte[] loadBackground(){
        try(InputStream image = getClass().getResourceAsStream(BACKGROUND)){
            if(image == null){
                throw new FailedToCreateCertificate("Imagem de background não encontrada");
            }
            return image.readAllBytes();
        }catch (IOException e){
            throw new FailedToCreateCertificate("Falha ao ler o Imagem de background", e);
        }
    }
}
