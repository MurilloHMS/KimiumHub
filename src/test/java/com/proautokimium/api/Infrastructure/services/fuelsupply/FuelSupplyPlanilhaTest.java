package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.domain.entities.FuelSupply;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O modelo que a gente entrega e o importador que lê de volta.
 *
 * <p><b>O defeito que este arquivo existe para impedir.</b> O
 * {@code FuelSupplyWriterService} anuncia os cabeçalhos <i>por nome</i>, e o
 * {@code FuelSupplyReaderService} lê os valores <i>por índice</i>
 * ({@code getString(row, 0)}, {@code getString(row, 3)}…). Os dois estão
 * acoplados pela <b>posição</b>, e nada no compilador liga um ao outro.
 *
 * <p>Duas colunas do modelo não são lidas por ninguém: <b>Cidade</b> (2) e
 * <b>Custo por Km</b> (10) — não existe campo para elas em {@code FuelSupply}.
 * Tirá-las de {@code getHeaders()} parece limpeza inofensiva, e desloca todas
 * as colunas seguintes: quem preencher o modelo novo põe a UF onde o leitor
 * espera a placa, e <b>toda importação vira lixo em silêncio</b>, sem erro e
 * sem linha recusada.
 *
 * <p>Por isso o teste escreve os valores procurando a coluna <b>pelo nome do
 * cabeçalho</b>, e não por número: é o que faz o desalinhamento aparecer.
 */
class FuelSupplyPlanilhaTest {

    private final FuelSupplyWriterService writer = new FuelSupplyWriterService();
    private final FuelSupplyReaderService reader = new FuelSupplyReaderService();

    private static final LocalDate DATA = LocalDate.of(2026, 8, 14);

    /** Acha a coluna pelo texto do cabeçalho, como quem preenche a planilha faz. */
    private static int coluna(Row cabecalho, String titulo) {
        for (int i = 0; i < cabecalho.getLastCellNum(); i++) {
            if (titulo.equals(cabecalho.getCell(i).getStringCellValue())) {
                return i;
            }
        }
        throw new AssertionError("O modelo não tem a coluna \"" + titulo + "\"");
    }

    @Test
    @DisplayName("O que se preenche no modelo chega no campo certo da entidade")
    void idaEVolta() throws Exception {
        byte[] modelo = writer.writeTemplate();

        byte[] preenchido;
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(modelo));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.getSheetAt(0);
            Row cabecalho = sheet.getRow(0);
            Row linha = sheet.createRow(1);

            // Data como texto: é o formato que o ExcelReaderHelper aceita sem
            // depender de a célula estar marcada como data.
            linha.createCell(coluna(cabecalho, "Nome do Motorista")).setCellValue("JOAO DA SILVA");
            linha.createCell(coluna(cabecalho, "Data de Abastecimento")).setCellValue("14/08/2026");
            linha.createCell(coluna(cabecalho, "Cidade")).setCellValue("MARINGA");
            linha.createCell(coluna(cabecalho, "UF")).setCellValue("PR");
            linha.createCell(coluna(cabecalho, "Placa")).setCellValue("ABC1D23");
            linha.createCell(coluna(cabecalho, "Hodometro Atual")).setCellValue(123456);
            linha.createCell(coluna(cabecalho, "Tipo de Combustível")).setCellValue("DIESEL S10");
            linha.createCell(coluna(cabecalho, "Litros abastecidos")).setCellValue(87.5);
            linha.createCell(coluna(cabecalho, "Valor Total")).setCellValue(524.13);
            linha.createCell(coluna(cabecalho, "Valor por Litro")).setCellValue(5.99);
            linha.createCell(coluna(cabecalho, "Custo por Km")).setCellValue(1.23);
            linha.createCell(coluna(cabecalho, "Diferença Hodometro")).setCellValue(642.0);
            linha.createCell(coluna(cabecalho, "Média Km/L")).setCellValue(7.34);

            wb.write(bos);
            preenchido = bos.toByteArray();
        }

        List<FuelSupply> lidos = reader.getDataByExcel(new ByteArrayInputStream(preenchido));

        assertThat(lidos).hasSize(1);
        FuelSupply fs = lidos.getFirst();

        assertThat(fs.getDriverName()).isEqualTo("JOAO DA SILVA");
        assertThat(fs.getFuelSupplyDate()).isEqualTo(DATA);
        assertThat(fs.getUf())
                .as("se a UF vier 'ABC1D23', o cabecalho e os indices do leitor sairam de sincronia")
                .isEqualTo("PR");
        assertThat(fs.getPlate()).isEqualTo("ABC1D23");
        assertThat(fs.getActualHodometer()).isEqualTo(123456);
        assertThat(fs.getFuelType()).isEqualTo("DIESEL S10");
        assertThat(fs.getLiters()).isEqualTo(87.5);
        assertThat(fs.getTotalValue()).isEqualTo(524.13);
        assertThat(fs.getPrice()).isEqualTo(5.99);
        assertThat(fs.getDiferenceHodometer())
                .as("a diferenca de hodometro vem da planilha, nao e calculada aqui")
                .isEqualTo(642.0);
        assertThat(fs.getAverageKm()).isEqualTo(7.34);
    }

    /**
     * As duas colunas sem destino são parte do contrato: elas existem para
     * manter as outras nas posições que o leitor espera. Documentado em teste
     * porque, sem isso, elas parecem sobra.
     */
    @Test
    @DisplayName("O modelo tem as 13 colunas, incluindo as duas que ninguem le")
    void oModeloTemAsTrezeColunas() throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(writer.writeTemplate()))) {
            Row cabecalho = wb.getSheetAt(0).getRow(0);

            assertThat((int) cabecalho.getLastCellNum()).isEqualTo(13);
            assertThat(coluna(cabecalho, "Cidade"))
                    .as("ninguem le esta coluna, e tira-la desloca UF, placa e todo o resto")
                    .isEqualTo(2);
            assertThat(coluna(cabecalho, "Custo por Km")).isEqualTo(10);
        }
    }
}
