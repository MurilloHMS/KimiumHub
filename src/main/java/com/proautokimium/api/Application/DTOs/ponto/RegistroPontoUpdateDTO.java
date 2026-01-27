package com.proautokimium.api.Application.DTOs.ponto;

import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record RegistroPontoUpdateDTO(
        @JsonFormat(pattern = "HH:mm")
        LocalTime entrada,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoSaida,
        @JsonFormat(pattern = "HH:mm")
        LocalTime almocoRetorno,
        @JsonFormat(pattern = "HH:mm")
        LocalTime saida
) {}