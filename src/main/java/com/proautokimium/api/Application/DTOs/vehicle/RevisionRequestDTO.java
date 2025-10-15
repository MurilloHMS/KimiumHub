package com.proautokimium.api.Application.DTOs.vehicle;

import java.time.LocalDate;

public record RevisionRequestDTO(LocalDate revisionDate, String vehiclePlate, int Kilometer, String nfe, String type, String driver, String observation, String localSystemCode) {}
