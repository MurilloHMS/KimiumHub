package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um cliente dentro do rascunho, com os números do mês já juntados.
 *
 * As colunas repetem as de {@link Newsletter} de propósito: na confirmação uma
 * vira a outra quase campo a campo. Separadas porque o rascunho pode ser
 * descartado, e a `newsletter` é a fila de envio — misturar as duas faria um
 * mês em revisão parecer um mês pronto para sair.
 */
@jakarta.persistence.Entity
@Table(name = "newsletter_previa_cliente")
@Getter
@Setter
@NoArgsConstructor
public class NewsletterPreviaCliente extends Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previa_id", nullable = false)
    private NewsletterPrevia previa;

    @Column(name = "codigo_cliente", nullable = false, length = 50)
    private String codigoCliente;

    @Column(name = "nome_do_cliente", nullable = false, length = 255)
    private String nomeDoCliente;

    /**
     * Pode ficar vazio: 31 dos 913 de junho não têm e-mail em lugar nenhum.
     *
     * A ordem de busca é cadastro de cliente primeiro, ERP como reserva — o
     * cadastro é a única fonte que sabe quem aceita receber.
     */
    @Column(name = "email_cliente", length = 255)
    private String emailCliente;

    /**
     * Quem está com `recebe_email = false` no cadastro fica de fora da fila,
     * **mas o registro é guardado marcado**: sumir com ele faria o total de
     * clientes do mês mudar sem explicação.
     */
    @Column(name = "recebe_email", nullable = false)
    private boolean recebeEmail = true;

    @Column(name = "codigo_matriz", length = 50)
    private String codigoMatriz;

    @Column(name = "nome_matriz", length = 255)
    private String nomeMatriz;

    @Column(name = "quantidade_notas_emitidas", nullable = false)
    private int quantidadeNotasEmitidas;

    @Column(name = "quantidade_de_produtos", nullable = false)
    private int quantidadeDeProdutos;

    @Column(name = "quantidade_de_litros", nullable = false)
    private double quantidadeDeLitros;

    @Column(name = "quantidade_de_visitas", nullable = false)
    private int quantidadeDeVisitas;

    @Column(name = "media_dias_atendimento")
    private int mediaDiasAtendimento;

    @Column(name = "produto_em_destaque", length = 255)
    private String produtoEmDestaque;

    @Column(name = "faturamento_total")
    private double faturamentoTotal;

    @Column(name = "valor_de_pecas_trocadas")
    private double valorDePecasTrocadas;

    @Column(name = "valor_total_horas_por_parceiro")
    private double valorTotalDeHoras;

    @Column(name = "valor_total_cobrado_hora")
    private double valorTotalCobradoHoras;

    @Column(name = "mau_uso")
    private boolean mauUso;

    @Column(name = "valor_total_horas_por_parceiro_mau_uso")
    private double valorTotalDeHorasMauUso;

    @Column(name = "valor_total_cobrado_hora_mau_uso")
    private double valorTotalCobradoHorasMauUso;
}
