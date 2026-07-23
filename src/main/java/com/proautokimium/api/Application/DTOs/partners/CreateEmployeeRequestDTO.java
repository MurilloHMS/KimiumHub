package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;
import com.proautokimium.api.domain.enums.humanResources.ContractType;

public record CreateEmployeeRequestDTO(
		String partnerCode,
		String document,
		String name,
		String email,
		Boolean ativo,
		String managerCode,
		Hierarchy hierarchy,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate birthday,
		Department department,
		UUID companyId,
		UUID teamId,
		UUID positionId,
		UUID positionLevelId,
		ContractType contractType,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate hiringDate
		) {}
