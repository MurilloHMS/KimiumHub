package com.proautokimium.api.Infrastructure.helpers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A data das planilhas importadas.
 *
 * Em setembro de 2026 a planilha de abastecimentos de agosto passou a trazer a
 * data como texto **com hora** — `28/08/2026 15:42:56`, nas 246 linhas. O
 * parse falhava, o `catch` devolvia `null` em silêncio, e a importação inteira
 * morria no `NOT NULL` de `fuelsupplydate`, com um 500 que falava de SQL e não
 * da célula.
 */
class ExcelReaderHelperTest {

    /** Célula de texto, como a planilha manda — sem arquivo, só na memória. */
    private static Cell celulaTexto(String valor) {
        Cell celula = new XSSFWorkbook().createSheet().createRow(0).createCell(0);
        celula.setCellValue(valor);
        return celula;
    }

    // ─── O defeito ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lê a data de um texto com hora — o formato da planilha de agosto")
    void leDataComHora() {
        LocalDate data = ExcelReaderHelper.returnLocalDateCellValue(celulaTexto("28/08/2026 15:42:56"));

        assertThat(data).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    // ─── O que já funcionava, e precisa continuar ────────────────────────────

    @Test
    @DisplayName("lê a data sem hora")
    void leDataSemHora() {
        assertThat(ExcelReaderHelper.returnLocalDateCellValue(celulaTexto("28/08/2026")))
                .isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    @DisplayName("lê dia e mês com uma casa só")
    void leDiaEMesComUmaCasa() {
        assertThat(ExcelReaderHelper.returnLocalDateCellValue(celulaTexto("1/8/2026")))
                .isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("aceita traço e ponto como separador")
    void aceitaOutrosSeparadores() {
        assertThat(ExcelReaderHelper.returnLocalDateCellValue(celulaTexto("28-08-2026")))
                .isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(ExcelReaderHelper.returnLocalDateCellValue(celulaTexto("28.08.2026")))
                .isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    @DisplayName("célula vazia devolve null")
    void celulaVazia() {
        assertThat(ExcelReaderHelper.returnLocalDateCellValue(null)).isNull();
    }
}
