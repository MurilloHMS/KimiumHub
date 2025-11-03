package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;

public record EmployeeDTO(
		String partnerCode,
		String document,
		String name,
		String email,
		Boolean ativo,
		String managerCode,
		Hierarchy hierarchy,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate birthday,
		Department department
		) {}
