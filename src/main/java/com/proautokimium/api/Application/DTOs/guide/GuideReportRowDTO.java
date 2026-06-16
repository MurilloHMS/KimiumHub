package com.proautokimium.api.Application.DTOs.guide;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.InputStream;
import java.util.List;

/**
 * DTO utilizado pelo relatório "Guia de Utilização".
 *
 * <p>Representa um produto e todas as informações necessárias para
 * preencher uma linha da tabela do relatório, incluindo imagens,
 * cores, finalidade, diluição, concentração, locais de uso e
 * equipamentos recomendados.</p>
 */
@Getter
@AllArgsConstructor
public class GuideReportRowDTO {

    private final String nome;
    private final String systemCode;
    private final InputStream imagemUrl;
    private final String coresHex;
    private final String finalidade;
    private final String descricao;
    private final String diluicao;
    private final String concentracao;
    private final String localUso;
    private final String equipamentos;
    private final List<InputStream> equipImagens;
    private InputStream circuloCorImagem;
}