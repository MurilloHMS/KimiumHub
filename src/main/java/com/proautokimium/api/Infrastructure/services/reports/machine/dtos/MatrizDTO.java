package com.proautokimium.api.Infrastructure.services.reports.machine.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatrizDTO {
    private String codMatriz;
    private String nomeMatriz;
    private BigDecimal totalMatriz;

    private List<UnidadeDTO> unidades;
}