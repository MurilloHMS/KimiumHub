package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;
import java.util.UUID;

import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;

public record EmployeeResponseDTO(
		UUID id,
		String partnerCode,
		String document,
		String name,
		String email,
		Boolean ativo,
		String managerCode,
		Hierarchy hierarchy,
		LocalDate birthday,
		Department department,
		UUID companyId,
		UUID teamId
		) {}
