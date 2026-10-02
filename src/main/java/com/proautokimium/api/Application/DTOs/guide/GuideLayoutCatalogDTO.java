package com.proautokimium.api.Application.DTOs.guide;

import java.util.List;

/**
 * O que o editor pode oferecer. Vem da API para o site não ter lista própria:
 * um campo novo no {@code GuideField} aparece no editor sem mexer no front.
 */
public record GuideLayoutCatalogDTO(List<Field> fields, List<Option> imageSources, List<String> fonts) {

    /** {@code kind}: TEXT, IMAGE ou IMAGE_LIST. */
    public record Field(String key, String label, String kind) {}

    public record Option(String key, String label) {}
}
