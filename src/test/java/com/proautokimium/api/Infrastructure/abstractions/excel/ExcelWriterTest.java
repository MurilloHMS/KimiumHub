package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * O molde das planilhas.
 *
 * <p>Nenhum teste cobria esta classe, e ela tem <b>três</b> subclasses — as duas
 * antigas com {@code writeDataRow} vazio, porque até 2026-09-24 só o
 * {@code writeTemplate()} era chamado (por
 * {@code MachineContractController:161}).
 */
class ExcelWriterTest {

    /** Uma subclasse mínima: o que se testa é o molde, não uma planilha real. */
    private static class PlanilhaDeTeste extends ExcelWriter<String> {
        @Override protected String getSheetName() { return "Teste"; }
        @Override protected String[] getHeaders() { return new String[]{"Nome", "Cidade"}; }

        @Override
        protected void writeDataRow(Row row, String item, Workbook workbook) {
            setCell(row, 0, item);
            setCell(row, 1, "Maringá");
        }
    }

    private final PlanilhaDeTeste planilha = new PlanilhaDeTeste();

    private static Sheet abrir(byte[] bytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            return wb.getSheetAt(0);
        }
    }

    /**
     * <b>O caminho que está em produção.</b> O download do modelo de contrato
     * chama isto, e a planilha que ele devolve tem que ter o cabeçalho e mais
     * nada.
     */
    @Test
    @DisplayName("writeTemplate devolve a planilha so com o cabecalho")
    void modeloTemSoOCabecalho() throws Exception {
        byte[] bytes = planilha.writeTemplate();

        Sheet sheet = abrir(bytes);
        assertThat(sheet.getLastRowNum())
                .as("so a linha 0, do cabecalho")
                .isZero();
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nome");
        assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Cidade");
    }

    /**
     * Lista vazia não é erro: um filtro que não achou nada é resultado
     * legítimo, e uma planilha só com cabeçalho é a resposta honesta. Recusar
     * aqui é o que quebra o {@code writeTemplate}, que é justamente uma
     * exportação de zero linhas.
     */
    @Test
    @DisplayName("Lista vazia gera planilha vazia, e nao excecao")
    void listaVaziaNaoEErro() {
        assertThatCode(() -> planilha.write(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cada item da lista vira uma linha depois do cabecalho")
    void cadaItemViraUmaLinha() throws Exception {
        byte[] bytes = planilha.write(List.of("Ana", "Bruno", "Carla"));

        Sheet sheet = abrir(bytes);
        assertThat(sheet.getLastRowNum()).isEqualTo(3);
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ana");
        assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Carla");
    }

    /**
     * {@code null} é erro de programação, e é diferente de "nenhum dado": lista
     * vazia tem resposta (planilha só com cabeçalho), nula não tem.
     *
     * <p>O teste não diz <b>qual</b> exceção — isso é escolha de quem escreve o
     * método. Diz só que ele não pode devolver uma planilha em silêncio.
     */
    @Test
    @DisplayName("Lista nula continua sendo recusada")
    void listaNulaERecusada() {
        assertThat(catchThrowable(() -> planilha.write(null)))
                .as("devolver planilha vazia para null esconde um bug de quem chamou")
                .isNotNull();
    }
}
