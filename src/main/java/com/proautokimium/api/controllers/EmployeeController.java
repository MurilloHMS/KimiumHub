package com.proautokimium.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Responsável pelo cadastro dos funcionários
 */
@RestController
@RequestMapping("api/employee")
@Tag(name = "Funcionários", description = "CRUD Funcionários")
public class EmployeeController {
	
	@Autowired
	EmployeeService service;
	
	@GetMapping
	@Operation(summary = "Obtém funcionários", description = "Retorna lista de funcionários")
	public ResponseEntity<?> getEmployes(){
		return service.getAllEmployes();
	}

	@GetMapping("only-email")
	@Operation(summary = "Obtém e-mails dos funcionários", description = "Retorna lista dos e-mails dos funcionários")
	public ResponseEntity<?> getEmployesEmail(){
		return service.getAllEmployesEmail();
	}

	/**
	 * Cria cadastro do funcionário
	 * @param dto Dados de cadastro
	 * @return Entidade Funcionário Cadastrado
	 */
	@PostMapping
	@Operation(summary = "Cria Funcionário", description = "Registra os dados do funcionário")
	public ResponseEntity<?> createEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createEmployee(dto));
	}

	/**
	 * Atualiza o cadastro do funcionário
	 * @param dto Dados de atualização
	 * @return Entidade Funcionário Atualizado
	 */
	@PutMapping
	@Operation(summary = "Atualiza Funcionário", description = "Atualiza os dados do funcionário")
	public ResponseEntity<?> updateEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		return ResponseEntity.status(HttpStatus.OK).body(service.updateEmployee(dto));
	}
}
