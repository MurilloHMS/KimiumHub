package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.reports.CertificateGeneratorReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/report")
public class ReportController {

    @Autowired
    CertificateGeneratorReport cgr;

    @GetMapping("/certificado")
    public ResponseEntity<byte[]> generateCertificate(){
        Map<String, Object> params = Map.of(

        );

        byte[] pdf = cgr.generate(params, "certificate.jasper");

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=certificado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
