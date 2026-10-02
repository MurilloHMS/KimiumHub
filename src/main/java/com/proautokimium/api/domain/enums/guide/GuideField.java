package com.proautokimium.api.domain.enums.guide;

import java.util.Arrays;
import java.util.Optional;

/**
 * O que pode ir dentro de uma célula do guia — e o único lugar que sabe disso.
 *
 * O editor do site não tem lista própria: ele pede este catálogo à API. Por
 * isso, uma propriedade nova do produto entra no guia com três passos — a
 * coluna em {@code products}, o campo no {@code GuideReportRowDTO} e uma linha
 * aqui. Os layouts já publicados não mudam: o campo novo só aparece quando o
 * designer o põe numa coluna.
 *
 * {@code rowField} é o nome da propriedade no {@code GuideReportRowDTO}, que o
 * Jasper lê como {@code $F{...}}. {@code fallback} é o que sai quando o produto
 * não tem o valor: "—" onde a ausência é informação ("sem diluição"), vazio
 * onde não é — e o bloco vazio some da célula.
 */
public enum GuideField {

    NAME("Nome do produto", Kind.TEXT, "nome", ""),
    CODE("Código", Kind.TEXT, "systemCode", "—"),
    COLOR_NAME("Nome da cor", Kind.TEXT, "corNome", ""),
    COLOR_CIRCLE("Círculo da cor", Kind.IMAGE, "circuloCorImagem", null),
    PHOTO("Foto do produto", Kind.IMAGE, "imagemUrl", null),
    PURPOSE("Finalidade", Kind.TEXT, "finalidade", "—"),
    DESCRIPTION("Descrição do guia", Kind.TEXT, "descricao", ""),
    DILUTION("Diluição", Kind.TEXT, "diluicao", "—"),
    CONCENTRATION("Concentração", Kind.TEXT, "concentracao", "—"),
    USAGE_AREA("Local de uso", Kind.TEXT, "localUso", "—"),
    EQUIPMENT_PHOTOS("Fotos dos equipamentos", Kind.IMAGE_LIST, "equipImagens", null),
    EQUIPMENT_NAMES("Nomes dos equipamentos", Kind.TEXT, "equipamentos", "—");

    public enum Kind { TEXT, IMAGE, IMAGE_LIST }

    private final String label;
    private final Kind kind;
    private final String rowField;
    private final String fallback;

    GuideField(String label, Kind kind, String rowField, String fallback) {
        this.label = label;
        this.kind = kind;
        this.rowField = rowField;
        this.fallback = fallback;
    }

    public String getLabel() { return label; }
    public Kind getKind() { return kind; }
    public String getRowField() { return rowField; }
    public String getFallback() { return fallback; }

    public boolean isImage() { return kind != Kind.TEXT; }

    public static Optional<GuideField> fromKey(String key) {
        return Arrays.stream(values()).filter(f -> f.name().equals(key)).findFirst();
    }
}
