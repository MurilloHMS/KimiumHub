package com.proautokimium.api.migrations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O Flyway lê {@code ${...}} como variável e recusa subir se ela não existir.
 *
 * Os testes rodam com o Flyway desligado (H2), e o psql aceita o arquivo —
 * então isto só aparecia no boot da API, com o deploy já em andamento. Foi o
 * que aconteceu com a V115: o dollar quote {@code $seed$} seguido do
 * {@code {} do JSON forma {@code ${}.
 */
class MigrationsHaveNoFlywayPlaceholderTest {

    @Test
    @DisplayName("nenhuma migration tem ${ — o Flyway trataria como variável e recusaria subir")
    void semPlaceholder() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/db/migration"))) {
            List<String> offenders = files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .filter(p -> read(p).contains("${"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
            assertThat(offenders).isEmpty();
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
