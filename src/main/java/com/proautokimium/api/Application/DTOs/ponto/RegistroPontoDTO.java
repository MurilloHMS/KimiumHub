package com.proautokimium.api.Application.DTOs.ponto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RegistroPontoDTO(
        UUID id,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate data,
        @JsonFormat(pattern = "HH:mm")
        LocalTime entrada,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoSaida,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoRetorno,
        @JsonFormat(pattern = "HH:mm")
        LocalTime saida,
        String horasTrabalhadas,
        UUID employeeId
) {}