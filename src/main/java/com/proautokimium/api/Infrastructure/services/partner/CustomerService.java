package com.proautokimium.api.Infrastructure.services.partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.domain.exceptions.customer.CustomerAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.valueObjects.Email;

import jakarta.transaction.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {
	
	@Autowired
	CustomerRepository repository;
	
	@Autowired
	PartnerReaderService reader;

    @Autowired
    ObjectMapper mapper;
	
	@Transactional
	public ResponseEntity<Object> createCustomer(CustomerRequestDTO dto){
        repository.findByCodParceiro(dto.codParceiro()).ifPresent(c -> {throw new CustomerAlreadyExistsException();
        });

		Customer newCustomer = Customer.fromDTO(dto);
		this.repository.save(newCustomer);
		return ResponseEntity.status(203).body("Parceiro criado com sucesso!");
	}
	
	@Transactional
	public ResponseEntity<Object> includeCustomersByExcel(MultipartFile file){
		try {
			List<Customer> customers = reader.getCustomersByExcel(file.getInputStream());
			
			if(customers.isEmpty()) {
                throw new CustomerNotFoundException();
			}
			
			List<String> partnersCode = customers.stream()
					.map(Customer::getCodParceiro)
					.toList();
			
			List<Customer> existingCustomers = repository.findByCodParceiroIn(partnersCode);
			
			Map<String, Customer> existingMap = existingCustomers.stream()
	                .collect(Collectors.toMap(Customer::getCodParceiro, c -> c));

	        List<Customer> toInsert = new ArrayList<>();
	        List<Customer> toUpdate = new ArrayList<>();
	        
	        for (Customer c : customers) {
	            if (existingMap.containsKey(c.getCodParceiro())) {
	                
	                Customer existing = existingMap.get(c.getCodParceiro());
	                existing.setEmail(c.getEmail());
	                toUpdate.add(existing);
	            } else {
	                toInsert.add(c);
	            }
	        }

	        if (!toInsert.isEmpty()) {
	            repository.saveAll(toInsert);
	        }

	        if (!toUpdate.isEmpty()) {
	            repository.saveAll(toUpdate);
	        }
	        
	        return ResponseEntity.ok(String.format(
	                "%d clientes adicionados, %d atualizados.",
	                toInsert.size(), toUpdate.size()
	            ));
		} catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Erro ao processar arquivo: " + e.getMessage());
	    }
	}
	
	public ResponseEntity<Object> getAllCustomers(){
		var customerList = this.repository.findAll().stream().map(c -> new CustomerRequestDTO(
				c.getCodParceiro(),
				c.getDocumento(),
				c.getName(),
				c.getUsername(),
				c.getEmail().getAddress(),
				c.isAtivo(),
				c.isRecebeEmail(),
				c.getCodigoMatriz())).toList();
		return customerList.isEmpty() 
				? ResponseEntity.noContent().build() 
				: ResponseEntity.ok(customerList);
	}
	
	@Transactional
	public ResponseEntity<Void> UpdateCustomer(CustomerRequestDTO dto){
        Customer customer = this.repository.findByCodParceiro(dto.codParceiro())
                .orElseThrow(CustomerNotFoundException::new);

        customer.setCodigoMatriz(dto.codMatriz());
        customer.setAtivo(dto.ativo());
        customer.setEmail(new Email(dto.email()));
        customer.setDocumento(dto.documento());
        customer.setRecebeEmail(dto.recebeEmail());
        customer.setName(dto.nome());

        this.repository.save(customer);
        return ResponseEntity.ok().build();
    }
	
	@Transactional
	public void DeleteCustomer(String codParceiro){
        Customer customer = this.repository.findByCodParceiro(codParceiro)
                .orElseThrow(CustomerNotFoundException::new);

        this.repository.delete(customer);
    }
}
