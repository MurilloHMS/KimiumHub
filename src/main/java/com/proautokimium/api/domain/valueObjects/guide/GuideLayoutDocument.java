package com.proautokimium.api.domain.valueObjects.guide;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * O layout do Guia de Utilização, como o designer monta na tela.
 *
 * É um documento, e vai inteiro para a coluna {@code guide_layouts.document}.
 * Quem transforma isto em PDF é o {@code GuideJasperDesignBuilder}: o Jasper
 * continua desenhando, só que a partir daqui e não mais de um {@code .jrxml}.
 *
 * Medidas em pontos (1/72 de polegada), as mesmas do Jasper. Posições do
 * cabeçalho e do rodapé são relativas à área útil (dentro das margens).
 *
 * Texto que tem lista fechada (formato, orientação, tipo de elemento, campo)
 * viaja como texto, e não como enum: um valor desconhecido vira uma frase do
 * {@code GuideLayoutValidator} dizendo onde está, e não um 500 na leitura.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideLayoutDocument(
        Page page,
        Band header,
        Table table,
        Band footer
) {

    /** {@code format}: LETTER ou A4. {@code orientation}: LANDSCAPE ou PORTRAIT. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(String format, String orientation, Margins margins, String font) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Margins(int top, int bottom, int left, int right) {}

    /** Cabeçalho ou rodapé: altura fixa e elementos em posição livre. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Band(int height, List<Element> elements) {}

    /**
     * Um item do cabeçalho ou do rodapé.
     *
     * {@code type}: TEXT, IMAGE, RECTANGLE ou LINE. Só os campos do tipo valem:
     * {@code text}/{@code fontSize}/{@code bold}/{@code verticalAlign} para texto,
     * {@code image}/{@code imageId} para imagem, {@code color} para todos menos
     * imagem. No texto, {@code {titulo}} vira o título que Contratos digita.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Element(
            String type,
            int x, int y, int width, int height,
            String text,
            Float fontSize,
            Boolean bold,
            String color,
            String align,
            String verticalAlign,
            String image,
            UUID imageId
    ) {}

    /**
     * A tabela dos produtos. Uma linha por produto, e a linha cresce se o texto
     * de alguma célula não couber em {@code minRowHeight}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Table(
            String headerBackground,
            String headerColor,
            float headerFontSize,
            int headerHeight,
            int minRowHeight,
            String dividerColor,
            String separatorColor,
            List<Column> columns
    ) {}

    /** {@code paddingTop}: o respiro antes do primeiro bloco da célula. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Column(String title, int width, int paddingTop, List<Block> blocks) {}

    /**
     * O que vai dentro da célula, empilhado de cima para baixo.
     *
     * {@code field} é uma chave de {@code GuideField}. Para texto,
     * {@code height} é a altura mínima, e a caixa cresce se o texto não couber.
     * Para imagem, é a altura da imagem. {@code count} só vale para as fotos
     * dos equipamentos: quantas, lado a lado.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Block(
            String field,
            int height,
            float fontSize,
            boolean bold,
            boolean uppercase,
            String align,
            String verticalAlign,
            String color,
            Integer count
    ) {}
}
