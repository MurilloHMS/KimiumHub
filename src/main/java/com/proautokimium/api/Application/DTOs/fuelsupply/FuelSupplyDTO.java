package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.time.LocalDate;

import com.proautokimium.api.domain.enums.Department;

public record FuelSupplyDTO(LocalDate fuelSupplyDate,
								String uf,
								 String plate,
								 String driverName,
								 Department department,
								 double actualHodometer,
								 double lastHodometer,
								 double diferenceHodometer,
								 double averageKm,
								 String fuelType,
								 double price,
								 double totalValue) {}
