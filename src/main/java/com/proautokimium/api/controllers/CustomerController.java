package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.cliente.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.domain.entities.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/customer")
public class CustomerController {

    @Autowired
    CustomerRepository repository;

    @PostMapping
    public ResponseEntity<Void> CreateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO customer){
        if(this.repository.findByCodParceiro(customer.codParceiro()) != null) return ResponseEntity.badRequest().build();

        Customer newCustomer = Customer.fromDTO(customer);

        this.repository.save(newCustomer);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Customer>> GetAllCustumer(){
        var customerList = this.repository.findAll();
        return ResponseEntity.ok(customerList);
    }
}
