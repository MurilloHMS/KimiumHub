package com.proautokimium.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proautokimium.api.Application.DTOs.partners.CreateEmployeeRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeResponseDTO;
import com.proautokimium.api.Application.DTOs.partners.PartnerRecipientDTO;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Responsável pelo cadastro dos funcionários
 */
@RestController
@RequestMapping("api/employee")
@Tag(name = "Funcionários", description = "CRUD Funcionários")
public class EmployeeController {

	private final EmployeeService service;

	public EmployeeController(EmployeeService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Obtém funcionários", description = "Retorna lista de funcionários")
	public ResponseEntity<List<EmployeeResponseDTO>> getEmployes(){
		return ResponseEntity.ok(service.getAllEmployes());
	}

	@GetMapping("only-email")
	@Operation(summary = "Obtém e-mails dos funcionários", description = "Retorna lista dos e-mails dos funcionários")
	public ResponseEntity<List<PartnerRecipientDTO>> getEmployesEmail(){
		return ResponseEntity.ok(service.getAllEmployesEmail());
	}

	/**
	 * Cria cadastro do funcionário
	 * @param dto Dados de cadastro
	 * @return Entidade Funcionário Cadastrado
	 */
	@PostMapping
	@Operation(summary = "Cria Funcionário", description = "Registra os dados do funcionário")
	public ResponseEntity<EmployeeResponseDTO> createEmploye(@RequestBody @Valid @NotNull CreateEmployeeRequestDTO dto){
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createEmployee(dto));
	}

	/**
	 * Atualiza o cadastro do funcionário
	 * @param dto Dados de atualização
	 * @return Entidade Funcionário Atualizado
	 */
	@PutMapping
	@Operation(summary = "Atualiza Funcionário", description = "Atualiza os dados do funcionário")
	public ResponseEntity<EmployeeResponseDTO> updateEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		return ResponseEntity.status(HttpStatus.OK).body(service.updateEmployee(dto));
	}
}
