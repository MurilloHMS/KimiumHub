package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.cliente.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.validation.OverridesAttribute;
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

    @PutMapping
    public ResponseEntity<Void> UpdateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO dto){
        var customer = this.repository.findByCodParceiro(dto.codParceiro());
        if(customer == null) return ResponseEntity.notFound().build();

        customer.setCodigoMatriz(dto.codMatriz());
        customer.setAtivo(dto.ativo());
        customer.setEmail(new Email(dto.email()));
        customer.setDocumento(dto.documento());
        customer.setRecebeEmail(dto.recebeEmail());

        this.repository.save(customer);
        return ResponseEntity.ok().build();
    }
}
