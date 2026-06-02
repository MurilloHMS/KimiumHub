package com.proautokimium.api.Application.DTOs.machine;

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
    private String vencimento;

    private List<UnidadeDTO> unidades;
}