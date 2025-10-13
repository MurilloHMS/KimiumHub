package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;

import com.proautokimium.api.domain.enums.Hierarchy;

public record EmployeeDTO(
		String partnerCode,
		String document,
		String name,
		String email,
		boolean ativo,
		String managerCode,
		Hierarchy hierarchy,
		LocalDate birthday
		) {}
