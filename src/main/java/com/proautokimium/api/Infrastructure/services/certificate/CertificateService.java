package com.proautokimium.api.Infrastructure.services.certificate;

import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.interfaces.certificate.CertificateGenerator;
import com.proautokimium.api.Infrastructure.services.reports.CertificateGeneratorReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class CertificateService implements CertificateGenerator {

    @Autowired
    CertificateGeneratorReport certificateGeneratorReport;

    @Override
    public byte[] generateCertificate(String name) {

        InputStream backgroundImage = getClass()
                .getResourceAsStream("/templates/images/certificado/certificado_padrao.png");

        if (backgroundImage == null) {
            throw new FailedToCreateCertificate("Imagem de background não encontrada");
        }

        Map<String,Object> params = new HashMap<>();
        params.put("NAME",name);
        params.put("BACKGROUND_IMAGE",backgroundImage);
        return certificateGeneratorReport.generate(params, "certificate.jrxml");
    }
}
