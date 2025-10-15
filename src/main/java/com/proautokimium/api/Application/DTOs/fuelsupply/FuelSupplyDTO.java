package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.time.LocalDate;

public record FuelSupplyDTO(LocalDate fuelSupplyDate,
								String uf,
								 String plate,
								 String driverName,
								 String department,
								 double actualHodometer,
								 double lastHodometer,
								 double diferenceHodometer,
								 double averageKm,
								 String fuelType,
								 double price,
								 double totalValue) {}
