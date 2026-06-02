package com.proautokimium.api.Application.DTOs.machine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatrizPreviewDTO {
    private String codMatriz;
    private String nomeMatriz;
    private int totalUnidades;
    private int totalMaquinas;
    private BigDecimal totalMatriz;
}
