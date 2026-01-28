package com.proautokimium.api.Application.DTOs.ponto;

import java.time.LocalDate;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

public record RegistroPontoRequestDTO(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate data,
        @JsonFormat(pattern = "HH:mm")
        LocalTime entrada,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoSaida,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoRetorno,
        @JsonFormat(pattern = "HH:mm")
        LocalTime saida
) {}