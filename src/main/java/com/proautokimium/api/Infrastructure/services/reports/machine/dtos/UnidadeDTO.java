package com.proautokimium.api.Infrastructure.services.reports.machine.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UnidadeDTO {
    private String numnota;
    private String nomeparc;
    private String cgcCpf;
    private String entrega;
    private BigDecimal vlrDesdob;

    private List<MaquinaDTO> maquinas;
}