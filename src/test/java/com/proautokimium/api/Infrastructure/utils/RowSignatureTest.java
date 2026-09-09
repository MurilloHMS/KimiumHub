package com.proautokimium.api.Infrastructure.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A assinatura de uma linha do ERP.
 *
 * <p>Ela existe porque a conciliação não guarda prévia em tabela. A tela mostra
 * o que o Sankhya disse, a pessoa marca o que quer aplicar, e o aplicar
 * <b>reconsulta o ERP</b> — se a linha mudou nesse meio-tempo, a assinatura não
 * bate e a gravação é recusada.
 *
 * <p><b>O que ela protege:</b> alguém abre a prévia, sai para o almoço, e volta
 * para aplicar. A alternativa a isto seria gravar o que o navegador mandou, e aí
 * a aba de ontem escreve dado velho sem nada avisar.
 */
class RowSignatureTest {

    // ── Estabilidade: a mesma linha dá sempre o mesmo resultado ───────────────

    @Test
    @DisplayName("a mesma linha gera sempre a mesma assinatura")
    void sameRowSameSignature() {
        String a = RowSignature.of("ACME LTDA", "12345678000199", "acme@x.com", "1708", "S");
        String b = RowSignature.of("ACME LTDA", "12345678000199", "acme@x.com", "1708", "S");

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("qualquer campo diferente muda a assinatura")
    void anyFieldChangesIt() {
        String base = RowSignature.of("ACME", "123", "a@x.com", "1708", "S");

        assertThat(RowSignature.of("ACME S/A", "123", "a@x.com", "1708", "S")).isNotEqualTo(base);
        assertThat(RowSignature.of("ACME", "999", "a@x.com", "1708", "S")).isNotEqualTo(base);
        assertThat(RowSignature.of("ACME", "123", "b@x.com", "1708", "S")).isNotEqualTo(base);
        assertThat(RowSignature.of("ACME", "123", "a@x.com", "505", "S")).isNotEqualTo(base);
        assertThat(RowSignature.of("ACME", "123", "a@x.com", "1708", "N"))
                .as("o ativo entra: é o que faz o balde de inativos existir")
                .isNotEqualTo(base);
    }

    /**
     * <b>O separador é o que impede duas linhas diferentes de virarem a mesma.</b>
     *
     * <p>Concatenados sem nada no meio, {@code ("AB","C")} e {@code ("A","BC")}
     * produzem o mesmo texto — e a mesma assinatura. Duas linhas distintas
     * passariam por iguais, e a proteção sumiria justo onde deveria agir.
     *
     * <p>É o único defeito desta classe que falha em silêncio: nada estoura, o
     * teste de estabilidade continua verde, e uma alteração é aceita como se
     * nada tivesse mudado.
     */
    @Test
    @DisplayName("campos deslocados não colidem — o separador existe por isto")
    void separatorPreventsCollision() {
        assertThat(RowSignature.of("AB", "C"))
                .isNotEqualTo(RowSignature.of("A", "BC"));

        assertThat(RowSignature.of("ACME", ""))
                .isNotEqualTo(RowSignature.of("", "ACME"));
    }

    // ── Normalização ─────────────────────────────────────────────────────────

    /**
     * As colunas do {@code TGFPAR} são {@code CHAR} e vêm com espaço à direita.
     * Se um caminho aparar e o outro não, a assinatura muda sem o dado ter
     * mudado — e toda linha vira "alterada no ERP".
     */
    @Test
    @DisplayName("espaço nas pontas não conta")
    void trimsTheEdges() {
        assertThat(RowSignature.of("  ACME  ", " 123 "))
                .isEqualTo(RowSignature.of("ACME", "123"));
    }

    /**
     * Nulo e vazio são a mesma coisa: campo ausente.
     *
     * <p>68 dos 2008 clientes da consulta vêm sem e-mail, e o
     * {@code LinhaSankhya.texto()} devolve {@code null} nesses casos — de
     * propósito, para distinguir "não veio" de "veio vazio". Se os dois gerassem
     * assinaturas diferentes, esses clientes oscilariam entre duas e seriam
     * recusados no aplicar sem nada ter mudado.
     */
    @Test
    @DisplayName("nulo e vazio dão a mesma assinatura")
    void nullAndEmptyAreTheSame() {
        assertThat(RowSignature.of("ACME", null, "a@x.com"))
                .isEqualTo(RowSignature.of("ACME", "", "a@x.com"));

        assertThat(RowSignature.of("ACME", null, "a@x.com"))
                .as("só espaço também é campo ausente")
                .isEqualTo(RowSignature.of("ACME", "   ", "a@x.com"));
    }

    @Test
    @DisplayName("campo ausente não é o mesmo que campo com conteúdo")
    void absentIsNotContent() {
        assertThat(RowSignature.of("ACME", null))
                .isNotEqualTo(RowSignature.of("ACME", "123"));
    }

    // ── Formato ──────────────────────────────────────────────────────────────

    /**
     * Curta o bastante para viajar na tela em 2008 linhas, longa o bastante
     * para a colisão ser desprezível. 12 hex são 48 bits.
     */
    @Test
    @DisplayName("o resultado é hex curto e estável no tamanho")
    void shortHex() {
        String s = RowSignature.of("ACME", "123", "a@x.com", "1708", "S");

        assertThat(s).hasSize(12);
        assertThat(s).matches("[0-9a-f]{12}");
    }

    /** Sem argumento nenhum ainda produz algo — não estoura no caminho vazio. */
    @Test
    @DisplayName("lista vazia não estoura")
    void emptyDoesNotThrow() {
        assertThat(RowSignature.of()).hasSize(12);
    }
}
