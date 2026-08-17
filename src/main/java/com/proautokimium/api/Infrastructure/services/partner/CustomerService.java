package com.proautokimium.api.Infrastructure.services.partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.client.ClientUserDTO;
import com.proautokimium.api.Application.DTOs.partners.PartnerRecipientDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.domain.exceptions.customer.CustomerAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

	@Autowired
	UserRepository userRepository;

	@Autowired
	TokenAuthService tokenAuthService;

	@Autowired
	AuthEmailService authEmailService;

	@Transactional
	public void createCustomer(CustomerRequestDTO dto){
        repository.findByCodParceiro(dto.codParceiro()).ifPresent(c -> { throw new CustomerAlreadyExistsException(); });

		Customer newCustomer = Customer.fromDTO(dto);
		this.repository.save(newCustomer);
	}
	
	@Transactional
	public void includeCustomersByExcel(MultipartFile file){
		try {
			List<Customer> customers = reader.getDataByExcel(file.getInputStream());
			
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
					existing.setDocumento(c.getDocumento());
					existing.setName(c.getName());
					existing.setCodigoMatriz(c.getCodigoMatriz());
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
		} catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
	    }
	}
	
	public ResponseEntity<Object> getAllCustomers(){
		var customerList = this.repository.findAll().stream().map(c -> new CustomerRequestDTO(
				c.getCodParceiro(),
				c.getDocumento(),
				c.getName(),
				c.getEmail().getAddress(),
				c.getUsername(),
				c.isAtivo(),
				c.isRecebeEmail(),
				c.getCodigoMatriz(),
				c.isMatriz())).toList();
		return customerList.isEmpty() 
				? ResponseEntity.noContent().build() 
				: ResponseEntity.ok(customerList);
	}

	public ResponseEntity<Object> getAllCustomersEmail(){
		List<PartnerRecipientDTO> customerList = this.repository.findAll()
				.stream()
				.map(c -> new PartnerRecipientDTO(
					c.getId(),
					c.getName(),
					c.getEmail().getAddress(),
					"customer"
			)).toList();
		return  ResponseEntity.ok(customerList);
	}
	
	@Transactional
	public void UpdateCustomer(CustomerRequestDTO dto){
        Customer customer = this.repository.findByCodParceiro(dto.codParceiro())
                .orElseThrow(CustomerNotFoundException::new);

        customer.setCodigoMatriz(dto.codMatriz());
        customer.setAtivo(dto.ativo());
        customer.setEmail(new Email(dto.email()));
        customer.setDocumento(dto.documento());
        customer.setRecebeEmail(dto.recebeEmail());
        customer.setName(dto.nome());
		customer.setMatriz(dto.isMatriz());

        this.repository.save(customer);
    }
	
	@Transactional
	public void DeleteCustomer(String codParceiro){
        Customer customer = this.repository.findByCodParceiro(codParceiro)
                .orElseThrow(CustomerNotFoundException::new);

        this.repository.delete(customer);
    }

	public List<ClientUserDTO> listAccess(String codParceiro) {
		Customer customer = repository.findByCodParceiro(codParceiro)
				.orElseThrow(CustomerNotFoundException::new);

		return userRepository.findByCustomer_Id(customer.getId())
				.stream()
				.map(user -> new ClientUserDTO(user.getLogin(), user.getEmail(), user.isActive()))
				.toList();
	}

	/**
	 * O convite é o único caminho de acesso ao portal. CNPJ é dado público -
	 * está em toda nota fiscal -, então um primeiro acesso self-service como o
	 * do funcionário deixaria qualquer um pedir a conta de qualquer empresa.
	 */
	@Transactional
	public void inviteAccess(String codParceiro, String email) {
		Customer customer = repository.findByCodParceiro(codParceiro)
				.orElseThrow(CustomerNotFoundException::new);

		if (!customer.isAtivo()) {
			throw new AccessDeniedException("Cliente inativo não recebe acesso ao portal.");
		}

		if (userRepository.findByEmail(email) != null) {
			throw new UserAlreadyExistsException("Já existe um acesso com o e-mail " + email + ".");
		}

		String token = tokenAuthService.createInviteForPartner(customer, email);
		authEmailService.sendClientInvite(email, token, customer.getName());
	}
}
