package com.proautokimium.api.domain.enums.guide;

import java.util.Arrays;
import java.util.Optional;

/**
 * De onde vem a imagem de um elemento do cabeçalho ou do rodapé.
 *
 * As fixas ficam no classpath e o logo do cliente chega no pedido de geração.
 * {@code UPLOADED} é uma imagem que o designer enviou pela tela, guardada em
 * {@code guide_layout_images} e apontada pelo {@code imageId} do elemento.
 */
public enum GuideImageSource {

    COMPANY_LOGO("Logo da empresa", null),
    CUSTOMER_LOGO("Logo do cliente", null),
    PPE_MASK("EPI · Máscara", "/templates/images/icones-guia/mascara.png"),
    PPE_GOGGLES("EPI · Óculos", "/templates/images/icones-guia/oculos.png"),
    PPE_APRON("EPI · Avental", "/templates/images/icones-guia/avental.png"),
    PPE_CAP("EPI · Touca", "/templates/images/icones-guia/touca.png"),
    PPE_GLOVE("EPI · Luva", "/templates/images/icones-guia/luva.png"),
    PPE_BOOT("EPI · Bota", "/templates/images/icones-guia/bota.png"),
    UPLOADED("Imagem enviada", null);

    private final String label;
    private final String classpathResource;

    GuideImageSource(String label, String classpathResource) {
        this.label = label;
        this.classpathResource = classpathResource;
    }

    public String getLabel() { return label; }

    /** Nulo para as que não moram no classpath: logos e imagem enviada. */
    public String getClasspathResource() { return classpathResource; }

    public static Optional<GuideImageSource> fromKey(String key) {
        return Arrays.stream(values()).filter(s -> s.name().equals(key)).findFirst();
    }
}
