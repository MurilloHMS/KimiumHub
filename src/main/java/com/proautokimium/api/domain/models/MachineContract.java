package com.proautokimium.api.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MachineContract {
    private String numeroUnico;
    private String numeroNota;
    private String nomeParceiro;
    private String documento;
    private String codigoMatriz;
    private String nomeMatriz;
    private LocalDate dataNegociacao;
    private String codigoProduto;
    private String descricaoProduto;
    private Double vlrUnitario;
    private String observacao;
    private String numeroFinanceiro;
    private Double vlrDesdobramento;
    private String enderecoEntrega;
}
