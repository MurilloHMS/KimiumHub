package com.proautokimium.api.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Newsletter {
    private String codigoCliente;
    private String nomeDoCliente;
    private LocalDate data;
    private String mes;
    private int quantidadeDeProdutos;
    private double quantidadeDeLitros;
    private int quantidadeDeVisitas;
    private int quantidadeNotasEmitidas;
    private int mediaDiasAtendimento;
    private String produtoEmDestaque;
    private double faturamentoTotal;
    private double valorDePecasTrocadas;
    private String status;
    private boolean clienteCadastrado;
    private String emailCliente;
}
