package com.proautokimium.api.Application.DTOs.machine;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MaquinaDTO {
    private String codProd;
    private String  descrprod;
    private BigDecimal vlrunit;
    private String  observacao;
}