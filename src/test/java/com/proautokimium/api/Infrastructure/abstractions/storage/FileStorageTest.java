package com.proautokimium.api.Infrastructure.abstractions.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A base de armazenamento, e a travessia de caminho que ela não barra.
 *
 * <p><b>Escrito em 2026-09-11, antes da correção — nasce vermelho.</b>
 *
 * <p>{@code searchFile} faz {@code Paths.get(storagePath).resolve(filename)} sem
 * normalizar, e o {@code filename} vem de {@code @PathVariable} no
 * {@code CurriculoController}. Está atrás de authority, então não é anônimo —
 * mas qualquer conta do RH lê qualquer arquivo que a JVM alcance, e este
 * trabalho vai acrescentar um segundo caminho de download.
 *
 * <p>A correção vai na <b>classe base</b>, e não no {@code StorageService}:
 * são onze subclasses, e consertar uma deixa dez abertas.
 *
 * <p><b>Sobre o tipo da exceção:</b> {@code IllegalArgumentException} é sugestão
 * minha, não requisito. Se você escolher outro — {@code InfrastructureException}
 * é defensável, já que quem chamou não tem o que fazer a respeito —, me diga e
 * eu ajusto a asserção. O que não pode mudar é que ele <b>recusa</b>.
 */
class FileStorageTest {

    /** Subclasse mínima: o que se testa é a base, não uma implementação. */
    private static class ArmazenamentoDeTeste extends FileStorage {
        private final String caminho;

        ArmazenamentoDeTeste(String caminho) {
            this.caminho = caminho;
        }

        @Override protected String getStoragePath() { return caminho; }
        @Override protected String getReturnPath()  { return ""; }
    }

    @TempDir Path pasta;

    private FileStorage armazenamento;

    @BeforeEach
    void setUp() {
        armazenamento = new ArmazenamentoDeTeste(pasta.toString());
    }

    @Test
    @DisplayName("Nome com ../ e recusado em vez de sair da pasta")
    void recusaTravessiaDeCaminho() {
        assertThatThrownBy(() -> armazenamento.searchFile("../../../etc/passwd"))
                .as("hoje isto devolve um caminho fora da pasta, e o controller serve o arquivo")
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * O par do teste acima, e ele não é enfeite: uma guarda escrita com
     * {@code startsWith} sobre a base <b>não normalizada</b> recusa tudo,
     * inclusive nome legítimo — e aí o download inteiro para de funcionar sem
     * ninguém entender por quê.
     */
    @Test
    @DisplayName("Nome comum continua resolvendo dentro da pasta")
    void nomeComumContinuaFuncionando() throws Exception {
        Files.createFile(pasta.resolve("curriculo.pdf"));

        Path resolvido = armazenamento.searchFile("curriculo.pdf");

        assertThat(resolvido.normalize()).startsWith(pasta.toAbsolutePath().normalize());
        assertThat(Files.exists(resolvido)).isTrue();
    }

    /**
     * A variante que passa despercebida: o nome não tem {@code ..}, mas é
     * absoluto. {@code Path.resolve} com caminho absoluto <b>descarta a base
     * inteira</b> e devolve o absoluto — é travessia sem nenhum ponto.
     */
    @Test
    @DisplayName("Nome absoluto tambem e recusado")
    void recusaCaminhoAbsoluto() {
        String absoluto = Path.of(System.getProperty("java.io.tmpdir"), "segredo.txt")
                .toAbsolutePath().toString();

        assertThatThrownBy(() -> armazenamento.searchFile(absoluto))
                .as("resolve() com caminho absoluto ignora a base e devolve o absoluto")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
