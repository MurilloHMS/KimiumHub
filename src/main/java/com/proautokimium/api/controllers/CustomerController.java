package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.services.partner.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/customer")
public class CustomerController {

    @Autowired
    CustomerService service;

    @PostMapping
    public ResponseEntity<Object> CreateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO customer){
        ResponseEntity<Object> responseEntity = service.createCustomer(customer);
        return responseEntity;
    }
    
    @PostMapping("upload")
    public ResponseEntity<Object> createCustomersByExcel(@RequestParam MultipartFile file) throws Exception{
    	ResponseEntity<Object> responseEntity = service.includeCustomersByExcel(file);
    	return responseEntity;
    }

    @GetMapping
    public ResponseEntity<Object> GetAllCustomer(){
    	 ResponseEntity<Object> responseEntity = service.getAllCustomers();
    	 return responseEntity;
    }

    @PutMapping
    public ResponseEntity<Void> UpdateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO dto){
    	 ResponseEntity<Void> responseEntity = service.UpdateCustomer(dto);
    	 return responseEntity;
    }

    @DeleteMapping
    public ResponseEntity<Void> DeleteCustomer(@RequestBody @NotNull @Valid String codParceiro){
    	 ResponseEntity<Void> responseEntity = service.DeleteCustomer(codParceiro);
    	 return responseEntity;
    }
    
}
