package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.services.partner.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller para gerenciar cadastros dos clientes
 */
@RestController
@RequestMapping("api/customer")
@Tag(name = "Clientes", description = "CRUD Clientes")
public class CustomerController {

    @Autowired
    CustomerService service;

    /**
     * Recebe dados e registra cliente
     * @param customer DTO - Dados do cliente
     * @return HttpStatus Created (201)
     */
    @PostMapping
    @Operation(summary = "Cria cliente", description = "Registra novo cliente")
    public ResponseEntity<String> CreateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO customer){
        service.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cliente cadastrado com sucesso!");
    }

    /**
     * Recebe planilha e registra clientes
     * @param file Planilha Excel com dados dos clientes
     * @return HttpStatus Created (201)
     */
    @PostMapping("upload")
    @Operation(summary = "Cria cliente via Excel", description = "Cadastra clientes via planilha")
    public ResponseEntity<String> createCustomersByExcel(@RequestParam MultipartFile file){
        service.includeCustomersByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).body("Clientes cadastrado com sucesso via planilha!");
    }

    /**
     * Obtém lista de clientes
     * @return Lista de clientes
     */
    @GetMapping
    @Operation(summary = "Obtém lista de clientes", description = "Retorna lista de cadastro dos clientes")
    public ResponseEntity<Object> GetAllCustomer(){
        return service.getAllCustomers();
    }

    /**
     * Obtém lista de e-mail dos clientes
     * @return Lista de E-mails
     */
    @GetMapping("only-email")
    @Operation(summary = "Obtém lista de emails", description = "Retorna lista de emails dos clientes")
    public ResponseEntity<Object> GetAllCustomerEmail(){
        return service.getAllCustomersEmail();
    }

    /**
     * Recebe dados para atualização do cadastro do cliente
     * @param dto Dados para atualizar cliente
     * @return HttpStatus OK (200)
     */
    @PutMapping
    @Operation(summary = "Atualiza Cliente", description = "Recebe dados para atualizar cliente")
    public ResponseEntity<String> UpdateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO dto){
        service.UpdateCustomer(dto);
        return ResponseEntity.status(HttpStatus.OK).body("Cliente atualizado com sucesso!");
    }

    /**
     * Recebe ID do parceiro e excluí cadastro
     * @param codParceiro Código interno Sankhya
     * @return HttpStatus OK (200)
     */
    @DeleteMapping
    @Operation(summary = "Deleta cliente", description = "Excluí do sistema o registro do cliente")
    public ResponseEntity<String> DeleteCustomer(@RequestBody @NotNull @Valid String codParceiro){
    	 service.DeleteCustomer(codParceiro);
    	 return ResponseEntity.status(HttpStatus.OK).body("Cliente deletado com sucesso!");
    }
    
}
