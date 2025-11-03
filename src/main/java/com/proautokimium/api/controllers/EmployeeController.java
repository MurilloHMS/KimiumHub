package com.proautokimium.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/employee")
public class EmployeeController {
	
	@Autowired
	EmployeeService service;
	
	@GetMapping
	public ResponseEntity<?> getEmployes(){
		return service.getAllEmployes();
	}
	
	@PostMapping
	public ResponseEntity<?> createEmploye(@RequestBody @Valid @Nonnull EmployeeDTO dto){
		return service.createEmployee(dto);
	}
	
	@PutMapping
	public ResponseEntity<?> updateEmploye(@RequestBody @Valid @Nonnull EmployeeDTO dto){
		return service.updateEmployee(dto);
	}
}
