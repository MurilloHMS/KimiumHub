package com.proautokimium.api.Application.DTOs.guide;

import java.io.InputStream;
import java.util.List;

/**
 * DTO de linha para o relatório "Guia de Utilização".
 *
 * <p>
 * Cada instância representa um produto e seus dados
 * para preencher uma linha da tabela no relatório JasperReports.
 * </p>
 *
 * @param nome          Nome comercial do produto
 * @param systemCode    Código do sistema (ex: "75033")
 * @param imagemUrl     Stream da imagem do produto (null = sem imagem)
 * @param coresHex      Hexadecimais das cores separados por vírgula (ex: "#1E90FF,#FF8C00")
 * @param finalidade    Descrição da finalidade do produto
 * @param diluicao      Modo/tipo de diluição (ex: "AUTOMÁTICA")
 * @param concentracao  Concentração de uso (ex: "10%")
 * @param localUso      Local(is) de uso (ex: "AÇOUGUE / PADARIA")
 * @param equipamentos  Nomes dos equipamentos separados por vírgula
 * @param equipImagens  Lista de streams das imagens dos equipamentos
 */
public record GuideReportRowDTO(
        String nome,
        String systemCode,
        InputStream imagemUrl,
        String coresHex,
        String finalidade,
        String diluicao,
        String concentracao,
        String localUso,
        String equipamentos,
        List<InputStream> equipImagens
) {}