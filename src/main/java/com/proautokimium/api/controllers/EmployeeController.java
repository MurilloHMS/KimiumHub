package com.proautokimium.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
	public ResponseEntity<?> createEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		var response = service.createEmployee(dto);
		return response;
	}
	
	@PutMapping
	public ResponseEntity<?> updateEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		return service.updateEmployee(dto);
	}
}
