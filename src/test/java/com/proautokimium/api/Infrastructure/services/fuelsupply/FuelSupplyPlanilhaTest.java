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
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * O modelo que a gente entrega e o importador que le de volta.
 *
 * <p><b>O defeito que este arquivo existe para impedir.</b> O leitor casava
 * coluna por <i>posicao</i> ({@code getString(row, 3)}), e a posicao e do
 * arquivo, nao nossa. Em 2026-09-24 a planilha da fornecedora chegou <b>sem a
 * coluna Cidade</b>, que o nosso modelo tem: da terceira coluna em diante tudo
 * andou uma casa, a UF passou a ler a placa, a placa passou a ler o hodometro,
 * e o combustivel caiu onde se esperava numero.
 *
 * <p>Agora o leitor casa pelo <b>nome do cabecalho</b>, com apelidos para os
 * dois arquivos que circulam aqui. Os testes cobrem os dois: o nosso modelo
 * preenchido a mao, e a exportacao da fornecedora como ela chega.
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
     * O modelo continua com as 13 colunas que as pessoas ja conhecem.
     *
     * <p>Duas delas nao tem destino: <b>Cidade</b> e <b>Custo por Km</b>. Elas
     * existiam para manter as outras na posicao que o leitor esperava; hoje o
     * leitor casa por nome e elas nao seguram mais nada. Ficam porque o arquivo
     * ja esta na mao das pessoas, e tirar coluna de um modelo em uso e decisao
     * de quem usa, nao limpeza de quem le.
     */
    @Test
    @DisplayName("O modelo continua com as 13 colunas conhecidas")
    void oModeloTemAsTrezeColunas() throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(writer.writeTemplate()))) {
            Row cabecalho = wb.getSheetAt(0).getRow(0);

            assertThat((int) cabecalho.getLastCellNum()).isEqualTo(13);
            assertThat(coluna(cabecalho, "Cidade")).isNotNegative();
            assertThat(coluna(cabecalho, "Custo por Km")).isNotNegative();
        }
    }

    // ───────────────────────────── a planilha da fornecedora ─────────────────

    /**
     * <b>A planilha que quebrou em 2026-09-24.</b> Sao os cabecalhos e a
     * primeira linha do arquivo de agosto, como ele chega: 14 colunas, nomes
     * diferentes dos nossos e <b>sem Cidade</b>.
     *
     * <p>Contra o leitor por posicao, esta linha dava
     * {@code For input string: "Gasolina Comum"} -- o combustivel caindo na
     * coluna do hodometro.
     */
    private static byte[] planilhaDaFornecedora(String... semEstaColuna) throws Exception {
        String[] cabecalhos = {
                "Nome do condutor",
                "Data/Hora transação (fim do abastecimento)",
                "Estado do posto",
                "Placa",
                "Hodômetro",
                "Combustível",
                "Litros",
                "Total do abastecimento",
                "R$/Litro",
                "R$/KM",
                "KM rodado",
                "KM/Litro",
                "Valor unitário",
                "Perfil"
        };
        Object[] valores = {
                "Aila Maria Serafim",
                "28/08/2025 09:19:59",
                "SP",
                "FCE3C61",
                102454.0,
                "Gasolina Comum",
                37.39,
                242.66,
                6.49,
                0.7703492063492063,
                315.0,
                8.424712489970581,
                6.49,
                "COMERCIAL"
        };

        List<String> fora = List.of(semEstaColuna);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Abastecimentos");
            Row cabecalho = sheet.createRow(0);
            Row linha = sheet.createRow(1);

            int destino = 0;
            for (int i = 0; i < cabecalhos.length; i++) {
                if (fora.contains(cabecalhos[i])) {
                    continue;
                }

                cabecalho.createCell(destino).setCellValue(cabecalhos[i]);

                if (valores[i] instanceof Double numero) {
                    linha.createCell(destino).setCellValue(numero);
                } else {
                    linha.createCell(destino).setCellValue((String) valores[i]);
                }
                destino++;
            }

            wb.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    @DisplayName("A planilha da fornecedora, sem a coluna Cidade, e lida no campo certo")
    void planilhaDaFornecedoraEhLidaCerto() throws Exception {
        List<FuelSupply> lidos = reader.getDataByExcel(
                new ByteArrayInputStream(planilhaDaFornecedora()));

        assertThat(lidos).hasSize(1);
        FuelSupply fs = lidos.getFirst();

        assertThat(fs.getDriverName()).isEqualTo("Aila Maria Serafim");
        assertThat(fs.getFuelSupplyDate()).isEqualTo(LocalDate.of(2025, 8, 28));
        assertThat(fs.getUf())
                .as("sem a coluna Cidade, o leitor por posicao lia a placa aqui")
                .isEqualTo("SP");
        assertThat(fs.getPlate()).isEqualTo("FCE3C61");
        assertThat(fs.getActualHodometer()).isEqualTo(102454);
        assertThat(fs.getFuelType())
                .as("era aqui que estourava: 'Gasolina Comum' caia na coluna do hodometro")
                .isEqualTo("Gasolina Comum");
        assertThat(fs.getLiters()).isEqualTo(37.39);
        assertThat(fs.getTotalValue()).isEqualTo(242.66);
        assertThat(fs.getPrice())
                .as("R$/Litro, e nao R$/KM: os dois sao numeros, e trocar nao estoura")
                .isEqualTo(6.49);
        assertThat(fs.getDiferenceHodometer()).isEqualTo(315);
        assertThat(fs.getAverageKm()).isEqualTo(8.424712489970581);
    }

    /**
     * <b>A mesma fornecedora, no download seguinte.</b> Estes sao os cabecalhos
     * e a primeira linha do arquivo baixado em 2026-09-24: 15 colunas, com
     * "Cidade do posto" de volta e "Valor bomba" no lugar de "Valor unitario".
     *
     * <p>Os dois arquivos sao do mesmo relatorio, baixados com duas semanas de
     * diferenca. E por isso que casar por posicao nao tem conserto: a posicao
     * nao e nossa, e ela muda sem aviso.
     */
    @Test
    @DisplayName("A exportacao seguinte, com Cidade do posto de volta, tambem e lida certo")
    void exportacaoComCidadeDoPosto() throws Exception {
        String[] cabecalhos = {
                "Nome do condutor",
                "Data/Hora transação (fim do abastecimento)",
                "Cidade do posto",
                "Estado do posto",
                "Placa",
                "Hodômetro",
                "Combustível",
                "Litros",
                "Total do abastecimento",
                "R$/Litro",
                "R$/KM",
                "KM rodado",
                "KM/Litro",
                "Valor bomba",
                "Perfil"
        };
        Object[] valores = {
                "Adilson Da Silva",
                "28/08/2026 15:42:56",
                "Porecatu",
                "PR",
                "TEB1B85",
                56592.0,
                "Gasolina Comum",
                27.39,
                191.46,
                6.99,
                0.453696682464455,
                422.0,
                15.407082876962397,
                191.46,
                "COMERCIAL"
        };

        byte[] planilha;

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Sheet1");
            Row cabecalho = sheet.createRow(0);
            Row linha = sheet.createRow(1);

            for (int i = 0; i < cabecalhos.length; i++) {
                cabecalho.createCell(i).setCellValue(cabecalhos[i]);

                if (valores[i] instanceof Double numero) {
                    linha.createCell(i).setCellValue(numero);
                } else {
                    linha.createCell(i).setCellValue((String) valores[i]);
                }
            }

            wb.write(bos);
            planilha = bos.toByteArray();
        }

        FuelSupply fs = reader.getDataByExcel(new ByteArrayInputStream(planilha)).getFirst();

        assertThat(fs.getDriverName()).isEqualTo("Adilson Da Silva");
        assertThat(fs.getFuelSupplyDate()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(fs.getUf())
                .as("\"Cidade do posto\" vem antes, e nao pode ser confundida com a UF")
                .isEqualTo("PR");
        assertThat(fs.getPlate()).isEqualTo("TEB1B85");
        assertThat(fs.getActualHodometer()).isEqualTo(56592);
        assertThat(fs.getFuelType()).isEqualTo("Gasolina Comum");
        assertThat(fs.getLiters()).isEqualTo(27.39);
        assertThat(fs.getTotalValue()).isEqualTo(191.46);
        assertThat(fs.getPrice())
                .as("R$/Litro, e nao R$/KM nem Valor bomba")
                .isEqualTo(6.99);
        assertThat(fs.getDiferenceHodometer()).isEqualTo(422);
        assertThat(fs.getAverageKm()).isEqualTo(15.407082876962397);
    }

    /**
     * Coluna que falta tem que dizer <b>qual</b>. "Erro ao ler a planilha" faz
     * a pessoa abrir o arquivo e comparar 14 colunas na mao.
     */
    @Test
    @DisplayName("Coluna que falta e recusada dizendo qual, e listando o que o arquivo tem")
    void colunaQueFaltaDizQual() throws Exception {
        byte[] semPlaca = planilhaDaFornecedora("Placa");

        Throwable erro = catchThrowable(() ->
                reader.getDataByExcel(new ByteArrayInputStream(semPlaca)));

        assertThat(erro).isNotNull();
        assertThat(erro.getMessage())
                .contains("placa")
                .contains("nome do condutor");
    }

    /**
     * O erro de celula tambem tem que dizer onde. Era
     * {@code For input string: "Gasolina Comum"}, que nao diz linha, nem
     * coluna, nem o que era esperado ali.
     */
    @Test
    @DisplayName("Texto numa coluna numerica aponta a linha e a coluna")
    void textoOndeSeEsperaNumero() throws Exception {
        byte[] planilha;

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(planilhaDaFornecedora()));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.getSheetAt(0);
            sheet.getRow(1)
                    .getCell(coluna(sheet.getRow(0), "Litros"))
                    .setCellValue("nao sei");

            wb.write(bos);
            planilha = bos.toByteArray();
        }

        Throwable erro = catchThrowable(() ->
                reader.getDataByExcel(new ByteArrayInputStream(planilha)));

        assertThat(erro).isNotNull();
        assertThat(erro.getMessage())
                .as("linha 2 da planilha, coluna G")
                .contains("Linha 2")
                .contains("coluna G")
                .contains("nao sei");
    }
}
