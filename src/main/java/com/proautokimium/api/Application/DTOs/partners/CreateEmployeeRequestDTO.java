package com.proautokimium.api.Application.DTOs.partners;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequestDTO(
		String partnerCode,
		String document,
		String name,
		String email,
		Boolean ativo,
		String managerCode,
		UUID hierarchyId,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate birthday,
		Department department,
		@NotNull(message = "Empresa é obrigatória")
		UUID companyId,
		@NotNull(message = "Setor é obrigatório")
		UUID teamId,
		@NotNull(message = "Cargo é obrigatório")
		UUID positionId,
		@NotNull(message = "Nível é obrigatório")
		UUID positionLevelId,
		@NotNull(message = "Tipo de contrato é obrigatório")
		ContractType contractType,
		@NotNull(message = "Data de admissão é obrigatória")
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate hiringDate,
		TransportType transportType,
		Integer dailyCommutesCount,
		Integer dailyMealsCount,
		BigDecimal ticketPrice,
		BigDecimal vehicleKmPerLiter,
		BigDecimal dailyDistanceKm
		) {}
