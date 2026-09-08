package com.proautokimium.api.Infrastructure.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A hora da ordem de serviço, escrita à mão.
 *
 * O campo é texto livre no Sankhya, e em junho de 2026 as 350 OS trouxeram
 * **duas convenções** — `13:00` em 267 delas e `14h20` em 79 — mais quatro
 * anotações que não são hora nenhuma.
 *
 * O SQL antigo usava `TRY_CAST(... AS TIME)`, que devolve `NULL` para `14h20`,
 * e o `WHERE ... IS NOT NULL` descartava a linha em silêncio: **82 das 350 OS
 * não entravam na conta de horas**, e o valor cobrado saía subestimado sem
 * nada na tela indicar.
 *
 * Por isso a regra aqui é a decisão dele: **o que tem uma leitura só, lê; o
 * que exige interpretação, vai para a correção manual.** Vazio não é erro — é
 * trabalho para a tela.
 */
class TimeParserUtilTest {

    // ─── O que precisa ser lido ───────────────────────────────────────────────

    @Test
    @DisplayName("lê o formato com dois-pontos — 267 das 350 OS de junho")
    void leDoisPontos() {
        assertThat(TimeParserUtil.interpret("13:00")).isPresent();
        assertThat(TimeParserUtil.interpret("08:50")).isPresent();
    }

    /** **O caso que o SQL antigo perdia.** */
    @Test
    @DisplayName("lê o formato com h — 79 das 350, que hoje somem")
    void leComH() {
        assertThat(TimeParserUtil.interpret("14h20")).isPresent();
        assertThat(TimeParserUtil.interpret("08h20")).isPresent();
    }

    @Test
    @DisplayName("as duas escritas da mesma hora dão o mesmo valor")
    void mesmaHoraDoisFormatos() {
        assertThat(TimeParserUtil.interpret("14h20"))
                .withFailMessage("14h20 e 14:20 sao a mesma hora")
                .isEqualTo(TimeParserUtil.interpret("14:20"));
    }

    // ─── O que precisa ir para a correção manual ──────────────────────────────

    @Test
    @DisplayName("vazio, nulo e só-espaços não são hora")
    void semTexto() {
        assertThat(TimeParserUtil.interpret(null)).isEmpty();
        assertThat(TimeParserUtil.interpret("")).isEmpty();
        assertThat(TimeParserUtil.interpret("   ")).isEmpty();
    }

    /**
     * **O teste que protege a decisão dele.** Um parser tolerante lê o começo
     * e devolve 05:00 — um número plausível, e ninguém pergunta. Casar a
     * string inteira é o que preserva a pergunta para a tela.
     */
    @Test
    @DisplayName("anotação livre não vira hora por acidente")
    void anotacaoLivreNaoViraHora() {
        assertThat(TimeParserUtil.interpret("5:00 horas"))
                .withFailMessage("pode ser 5h da manha ou 5 horas de trabalho — quem sabe e quem escreveu")
                .isEmpty();

        assertThat(TimeParserUtil.interpret("9:40 8/5")).isEmpty();
    }

    @Test
    @DisplayName("número sem separador não é hora")
    void numeroSolto() {
        assertThat(TimeParserUtil.interpret("1603"))
                .withFailMessage("pode ser 16:03, e pode ser um numero digitado no campo errado")
                .isEmpty();
    }

    // ─── O contrato ───────────────────────────────────────────────────────────

    /**
     * Vazio é resposta, não falha. Quem chama decide o que fazer — e aqui o que
     * se faz é mandar para a tela de correção.
     */
    @Test
    @DisplayName("nunca lança: texto ilegível devolve vazio")
    void nuncaLanca() {
        assertThatCode(() -> {
            TimeParserUtil.interpret("qualquer coisa");
            TimeParserUtil.interpret("13:00");
            TimeParserUtil.interpret("!@#$");
        }).doesNotThrowAnyException();
    }
}
