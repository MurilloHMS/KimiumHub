package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.certificate.CertificateHolderDTO;
import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.helpers.HttpHelper;
import com.proautokimium.api.Infrastructure.interfaces.certificate.CertificateGenerator;
import com.proautokimium.api.Infrastructure.repositories.CertificateHolderRepository;
import com.proautokimium.api.domain.entities.CertificateHolder;
import com.proautokimium.api.domain.exceptions.certificate.CertificateAlreadyExistsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller para geração dos certificados de treinamento.
 */
@RestController
@RequestMapping("api/certificate")
@Tag(name = "Gerador de certificados", description = "Gera certificados dos treinamentos internos")
public class CertificateController {

    @Autowired
    CertificateHolderRepository repository;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    CertificateGenerator generator;

    /**
     * Gera o certificado de treinamento com validação.
     * Se os dados do cliente já existir, retorna exceção
     * @param dto Dados do cliente para geração
     * @return Arquivo do certificado finalizado.
     *
     * @throws CertificateAlreadyExistsException Retorna conflict se o cliente já gerou um certificado.
     * @throws FailedToCreateCertificate Retorna Bad Request (500) caso ocorra um erro ao gerar o certificado.
     */
    @PostMapping
    @Operation(summary = "Cria certificado", description = "Cria o certificado do cliente e gera o registro")
    public ResponseEntity<?> createCertificateHolder(@RequestBody @NotNull @Valid CertificateHolderDTO dto){
        Optional<CertificateHolder> holder = repository.findByEmail(dto.email());
        if(holder.isPresent()){
            throw new CertificateAlreadyExistsException();
        }
        try{
            CertificateHolder entity = mapper.convertValue(dto, CertificateHolder.class);
            repository.save(entity);

            String name = dto.name().toUpperCase();
            String fileName = name + ".pdf";
            byte[] certificate = generator.generateCertificate(name);
            HttpHeaders headers = HttpHelper.createPdfHeader(fileName, certificate.length);

            return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(certificate);

        }catch (Exception e){
            throw new FailedToCreateCertificate("Erro ao criar o certificado: " + e.getMessage());
        }
    }

    /**
     * Gera o certificado de treinamento sem validação
     * @param dto Dados do cliente para geração
     * @return Arquivo do certificado finalizado.
     * @throws FailedToCreateCertificate Retorna Bad Request (500) caso ocorra um erro ao gerar o certificado.
     */
    @PostMapping("/no-validation")
    @Operation(summary = "Cria certificado", description = "Cria o certificado do cliente sem registro")
    public ResponseEntity<?> createCertificateHolderWithoutValidation(@RequestBody @NotNull @Valid CertificateHolderDTO dto){
        try{
            String fileName = dto.name().toUpperCase() + ".pdf";
            byte[] certificate = generator.generateCertificate(dto.name());
            HttpHeaders headers = HttpHelper.createPdfHeader(fileName, certificate.length);

            return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(certificate);

        }catch (Exception e){
            throw new FailedToCreateCertificate("Erro ao criar o certificado: " + e.getMessage());
        }
    }
}
