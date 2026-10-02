package com.proautokimium.api.Application.DTOs.guide;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * DTO utilizado pelo relatório "Guia de Utilização".
 *
 * <p>Representa um produto e todas as informações necessárias para
 * preencher uma linha da tabela do relatório, incluindo imagens,
 * cores, finalidade, diluição, concentração, locais de uso e
 * equipamentos recomendados.</p>
 *
 * <p>As imagens são bytes, e não streams: o designer pode pôr a mesma foto em
 * mais de um lugar, e bytes se leem quantas vezes for preciso. Com stream,
 * isso dependeria de o Jasper guardar a imagem depois da primeira leitura.</p>
 *
 * <p>O nome de cada propriedade é o que o {@code GuideField} aponta. Trocar um
 * nome aqui exige trocar lá.</p>
 */
@Getter
@AllArgsConstructor
public class GuideReportRowDTO {

    private final String nome;
    private final String systemCode;
    private final byte[] imagemUrl;
    private final String coresHex;
    private final String finalidade;
    private final String descricao;
    private final String diluicao;
    private final String concentracao;
    private final String localUso;
    private final String equipamentos;
    private final List<byte[]> equipImagens;
    private final byte[] circuloCorImagem;
    /** Nome(s) básico(s) da cor (ex.: "Azul", "Verde / Branco"); o hex cru quando não dá para nomear. */
    private final String corNome;
    /** Nomes dos equipamentos, na MESMA ordem das imagens em {@link #equipImagens}. */
    private final List<String> equipNomes;
}
