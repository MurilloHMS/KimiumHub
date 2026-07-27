package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
import java.math.BigDecimal;

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
		Department department,
		UUID companyId,
		UUID teamId,
		TransportType transportType,
		Integer dailyCommutesCount,
		BigDecimal ticketPrice,
		BigDecimal vehicleKmPerLiter,
		BigDecimal dailyDistanceKm
		) {}
