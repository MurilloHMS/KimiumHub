package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;
import java.util.UUID;

import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
import java.math.BigDecimal;

public record EmployeeResponseDTO(
		UUID id,
		String partnerCode,
		String document,
		String name,
		String email,
		Boolean ativo,
		String managerCode,
		UUID hierarchyId,
		LocalDate birthday,
		Department department,
		UUID companyId,
		UUID teamId,
		UUID positionId,
		UUID positionLevelId,
		String positionName,
		String positionLevelName,
		ContractType contractType,
		LocalDate hiringDate,
		BigDecimal salary,
		TransportType transportType,
		Integer dailyCommutesCount,
		Integer dailyMealsCount,
		BigDecimal ticketPrice,
		BigDecimal vehicleKmPerLiter,
		BigDecimal dailyDistanceKm,
		Integer vacationBalanceDays
		) {}
