package com.proautokimium.api.Application.DTOs.machine;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReciboLocacaoDTO {
    private String mesReferencia;
    private String vencimento;
    private String dataEmissao;
    private BigDecimal totalGeral;

    private List<MatrizDTO> matrizes;
}