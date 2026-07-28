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
public class UnidadePreviewDTO {
    private String numNota;
    private String nomeParceiro;
    private String documento;
    private String enderecoEntrega;
    private int quantidadeMaquinas;
    private BigDecimal vlrDesdobramento;
}
