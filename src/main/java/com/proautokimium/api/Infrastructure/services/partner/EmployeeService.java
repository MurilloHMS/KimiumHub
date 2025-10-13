package com.proautokimium.api.Infrastructure.services.partner;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.valueObjects.Email;

@Service
public class EmployeeService {

	private final EmployeeRepository repository;
	
	public EmployeeService(EmployeeRepository repository) {
		this.repository = repository;
	}
	
	public ResponseEntity<?> createEmployee(EmployeeDTO dto) {
		
		try {
			Employee employee = new Employee() {};
		
			employee.setCodParceiro(dto.partnerCode());
			employee.setCodigoGerente(dto.managerCode());
			employee.setAtivo(dto.ativo());
			employee.setBirthday(dto.birthday());
			employee.setDocumento(dto.document());
			employee.setEmail(new Email(dto.email()));
			employee.setHierarquia(dto.hierarchy());
			employee.setName(dto.name());
		
			repository.save(employee);
			return ResponseEntity.ok().body("Funcionário criado com sucesso");
		}catch (Exception e) {
			return ResponseEntity.badRequest().body("Ocorreu um erro ao criar o funcionário. Error Message: " + e.getMessage());
		}
	}
	
	public ResponseEntity<?> updateEmployee(EmployeeDTO dto){
		try {
			Employee employee = repository.findByCodParceiro(dto.partnerCode());
			
			employee.setCodigoGerente(dto.managerCode());
			employee.setAtivo(dto.ativo());
			employee.setBirthday(dto.birthday());
			employee.setDocumento(dto.document());
			employee.setEmail(new Email(dto.email()));
			employee.setHierarquia(dto.hierarchy());
			employee.setName(dto.name());
			
			repository.save(employee);
			
			return ResponseEntity.ok().body("Funcionário atualizado com sucesso");
		}catch (Exception e) {
			return ResponseEntity.badRequest().body("Ocorreu um erro ao criar o funcionário. Error Message: " + e.getMessage());
		}
	}
	
	public ResponseEntity<?> getAllEmployes(){
		
		try {
			List<Employee> employesList = repository.findAll();
			return ResponseEntity.ok(employesList);
		}catch (Exception e) {
			return ResponseEntity.badRequest().body("Ocorreu um erro ao obter a lista de funcionários. Error Message: " + e.getMessage());
		}
	}
}
