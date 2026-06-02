package com.proautokimium.api.Application.DTOs.machine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /**
     * Parágrafo 1 – Referência da locação.
     * Ex: "Refere-se à locação de 1 (Uma) Máquina de Lavar Louça – NT 810 - Esteira -
     *       locada pela empresa PROAUTO INDÚSTRIA QUÍMICA EIRELI com emissão de nota
     *       de remessa de comodato 5792."
     */
    private String textoIntro;

    /**
     * Parágrafo 2 – Localização da máquina.
     * Ex: "A máquina está instalada na empresa MENU ALIMENTAÇÃO – UNIDADE MASTER
     *       CETREVI com endereço de entrega: Rod. Videira Anta Gorda KM 05, Videira,
     *       CEP 89560-000."
     */
    private String textoInstalacao;

    /**
     * Parágrafo 3 – Valor e condição de pagamento.
     * Ex: "O valor da locação é de R$ 1.579,80 (Um mil e quinhentos e setenta e nove
     *       reais e oitenta centavos) com vencimento todo dia 24 de cada mês
     *       subsequente."
     */
    private String textoValor;

    /** Valor formatado em reais, ex: "R$ 1.579,80" */
    private String vlrFormatado;

    /** Número de máquinas nesta unidade (para exibição). */
    private int quantidadeMaquinas;
}