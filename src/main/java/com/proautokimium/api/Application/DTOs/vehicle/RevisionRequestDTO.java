package com.proautokimium.api.Application.DTOs.vehicle;

import java.time.LocalDate;

public record RevisionRequestDTO(LocalDate revisionDate,
		String vehiclePlate, 
		int kilometer, 
		String nfe, 
		String type, 
		String driver, 
		String observation,
		String localSystemCode) {}
