package com.proautokimium.api.Application.DTOs.machine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MatrizPreviewDTO {
    private String codMatriz;
    private String nomeMatriz;
    private int totalUnidades;
    private int totalMaquinas;
    private BigDecimal totalMatriz;
    private List<UnidadePreviewDTO> unidades = new ArrayList<>();

    public MatrizPreviewDTO(String codMatriz, String nomeMatriz, int totalUnidades,
                            int totalMaquinas, BigDecimal totalMatriz) {
        this.codMatriz = codMatriz;
        this.nomeMatriz = nomeMatriz;
        this.totalUnidades = totalUnidades;
        this.totalMaquinas = totalMaquinas;
        this.totalMatriz = totalMatriz;
    }
}
