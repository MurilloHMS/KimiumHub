package com.proautokimium.api.Application.DTOs.vehicle;

import java.time.LocalDate;
import java.util.UUID;

public record RevisionRequestDTO(LocalDate revisionDate, String vehiclePlate, int Kilometer, String nfe, String type, String driver, String observation, String local) {}
