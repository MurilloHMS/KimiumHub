package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * O layout semeado pela V115, lido da própria migration.
 *
 * Os testes rodam no H2 com o Flyway desligado, então a semente não está no
 * banco. Ler do arquivo — e não de uma cópia aqui — garante que o que se
 * testa é o que vai para produção.
 */
final class GuideLayoutSeed {

    static final String MIGRATION = "/db/migration/V115__layout_editavel_do_guia.sql";

    private GuideLayoutSeed() {}

    static String text() {
        try (InputStream in = GuideLayoutSeed.class.getResourceAsStream(MIGRATION)) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int start = sql.indexOf("VALUES ($seed$") + "VALUES ($seed$".length();
            int end = sql.indexOf("$seed$", start);
            return sql.substring(start, end);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static GuideLayoutDocument document() {
        try {
            return new ObjectMapper().readValue(text(), GuideLayoutDocument.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
