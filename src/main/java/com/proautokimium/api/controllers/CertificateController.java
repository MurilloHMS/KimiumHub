package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.certificateHolder.CertificateHolderDTO;
import com.proautokimium.api.Infrastructure.repositories.CertificateHolderRepository;
import com.proautokimium.api.domain.entities.CertificateHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api/certificate")
public class CertificateController {

    @Autowired
    CertificateHolderRepository repository;

    @Autowired
    ObjectMapper mapper;

    @PostMapping
    public ResponseEntity<?> createCertificateHolder(@RequestBody @NotNull @Valid CertificateHolderDTO dto){
        Optional<CertificateHolder> holder = repository.findByEmail(dto.email());
        if(holder.isPresent()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe uma emissão de certificado no email cadastrado!");
        }

        CertificateHolder entity = mapper.convertValue(dto, CertificateHolder.class);

        repository.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body("Certificado liberado com sucesso!");
    }
}
