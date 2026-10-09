package com.proautokimium.api.Application.DTOs.partners;

import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.SiteAccess;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.TransportType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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
		Integer vacationBalanceDays,
		SiteAccess siteAccess,
		String siteLogin,
		LocalDateTime firstAccessRequestedAt
		) {}
