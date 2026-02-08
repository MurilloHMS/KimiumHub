package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.services.partner.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/customer")
public class CustomerController {

    @Autowired
    CustomerService service;

    @PostMapping
    public ResponseEntity<String> CreateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO customer){
        service.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cliente cadastrado com sucesso!");
    }
    
    @PostMapping("upload")
    public ResponseEntity<String> createCustomersByExcel(@RequestParam MultipartFile file){
        service.includeCustomersByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).body("Clientes cadastrado com sucesso via planilha!");
    }

    @GetMapping
    public ResponseEntity<Object> GetAllCustomer(){
        return service.getAllCustomers();
    }

    @PutMapping
    public ResponseEntity<String> UpdateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO dto){
        service.UpdateCustomer(dto);
        return ResponseEntity.status(HttpStatus.OK).body("Cliente atualizado com sucesso!");
    }

    @DeleteMapping
    public ResponseEntity<String> DeleteCustomer(@RequestBody @NotNull @Valid String codParceiro){
    	 service.DeleteCustomer(codParceiro);
    	 return ResponseEntity.status(HttpStatus.OK).body("Cliente deletado com sucesso!");
    }
    
}
