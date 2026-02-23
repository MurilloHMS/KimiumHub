package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.certificateHolder.CertificateHolderDTO;
import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.interfaces.certificate.CertificateGenerator;
import com.proautokimium.api.Infrastructure.repositories.CertificateHolderRepository;
import com.proautokimium.api.domain.entities.CertificateHolder;
import com.proautokimium.api.domain.exceptions.certificate.CertificateAlreadyExistsException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Optional;

@RestController
@RequestMapping("api/certificate")
public class CertificateController {

    @Autowired
    CertificateHolderRepository repository;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    CertificateGenerator generator;

    @PostMapping
    public ResponseEntity<?> createCertificateHolder(@RequestBody @NotNull @Valid CertificateHolderDTO dto){
        Optional<CertificateHolder> holder = repository.findByEmail(dto.email());
        if(holder.isPresent()){
            throw new CertificateAlreadyExistsException();
        }

        CertificateHolder entity = mapper.convertValue(dto, CertificateHolder.class);

        repository.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body("Certificado liberado com sucesso!");
    }

    @PostMapping("{name}")
    public ResponseEntity<?> createCertificate(@PathVariable String name){
        try{
            String fileName = name.toUpperCase() + ".pdf";
            byte[] certificate = generator.generateCertificate(name);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(fileName)
                            .build()
            );

            return ResponseEntity.ok().headers(headers).body(certificate);
        }catch (Exception e){
            throw new FailedToCreateCertificate("Erro ao criar o certificado: " + e.getMessage());
        }
    }
}
