package com.proautokimium.api.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.enums.EmailStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "newsletter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Newsletter extends Entity{
	 	@Column(name = "codigo_cliente", nullable = false, length = 50)
	    private String codigoCliente;

	    @Column(name = "nome_do_cliente", nullable = false, length = 255)
	    private String nomeDoCliente;

	    @Column(name = "data", nullable = false)
	    private LocalDate data;

	    @Column(name = "mes", nullable = false, length = 20)
	    private String mes;

	    @Column(name = "quantidade_de_produtos", nullable = false)
	    private int quantidadeDeProdutos;

	    @Column(name = "quantidade_de_litros", nullable = false)
	    private double quantidadeDeLitros;

	    @Column(name = "quantidade_de_visitas", nullable = false)
	    private int quantidadeDeVisitas;

	    @Column(name = "quantidade_notas_emitidas", nullable = false)
	    private int quantidadeNotasEmitidas;

	    @Column(name = "media_dias_atendimento")
	    private int mediaDiasAtendimento;

	    @Column(name = "produto_em_destaque", length = 255)
	    private String produtoEmDestaque;

	    @Column(name = "faturamento_total")
	    private double faturamentoTotal;

	    @Column(name = "valor_de_pecas_trocadas")
	    private double valorDePecasTrocadas;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "status", length = 50)
	    private EmailStatus status;

	    @Column(name = "email_cliente", length = 255)
	    private String emailCliente;
}
