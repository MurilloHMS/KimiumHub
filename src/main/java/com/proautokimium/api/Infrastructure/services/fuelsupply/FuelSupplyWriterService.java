package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import com.proautokimium.api.domain.entities.FuelSupply;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * A planilha de abastecimentos, nos dois usos: o modelo em branco que a pessoa
 * baixa para preencher, e a exportação dos dados que já estão no sistema.
 *
 * <p><b>As duas saem com as mesmas 13 colunas, na mesma ordem</b>, porque o
 * arquivo exportado precisa voltar a ser um arquivo importável: quem exporta,
 * corrige uma linha e reenvia não pode descobrir no meio do caminho que o
 * formato de saída não é o de entrada.
 */
@Service
public class FuelSupplyWriterService extends ExcelWriter<FuelSupply> {

    /**
     * A data vai como <b>texto</b>, e não como data formatada.
     *
     * <p>Duas razões. O {@code ExcelReaderHelper} aceita texto neste formato,
     * então o arquivo exportado é lido de volta sem depender de a célula estar
     * marcada como data. E data formatada exigiria um {@code CellStyle}: estilo
     * pertence ao workbook, criar um por linha estoura o teto de 64 mil do xlsx
     * numa exportação grande, e o Template Method não tem onde guardar um
     * estilo entre as linhas sem pôr estado num {@code @Service} singleton.
     */
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected String getSheetName() {
        return "Abastecimentos";
    }

    @Override
    protected String[] getHeaders() {
        String[] values = new String[]{
                "Nome do Motorista",
                "Data de Abastecimento",
                "Cidade",
                "UF",
                "Placa",
                "Hodometro Atual",
                "Tipo de Combustível",
                "Litros abastecidos",
                "Valor Total",
                "Valor por Litro",
                "Custo por Km",
                "Diferença Hodometro",
                "Média Km/L"
        };

        return values;
    }

    /**
     * As colunas 2 (Cidade) e 10 (Custo por Km) saem <b>em branco</b>: não
     * existe campo para elas em {@code FuelSupply}, e o leitor nunca as leu.
     * Elas continuam aqui porque tirá-las deslocaria todas as colunas seguintes
     * — a UF cairia onde o leitor espera a placa.
     *
     * <p>Preencher "Custo por Km" com uma conta nossa seria inventar dado: o
     * número da planilha vem da fornecedora, não daqui.
     */
    @Override
    protected void writeDataRow(Row row, FuelSupply item, Workbook workbook) {
        setCell(row, 0, item.getDriverName());
        setCell(row, 1, item.getFuelSupplyDate() != null ? item.getFuelSupplyDate().format(DATA) : "");
        setCell(row, 2, "");
        setCell(row, 3, item.getUf());
        setCell(row, 4, item.getPlate());
        setCell(row, 5, item.getActualHodometer());
        setCell(row, 6, item.getFuelType());
        setCell(row, 7, item.getLiters());
        setCell(row, 8, item.getTotalValue());
        setCell(row, 9, item.getPrice());
        setCell(row, 10, "");
        setCell(row, 11, item.getDiferenceHodometer());
        setCell(row, 12, item.getAverageKm());
    }
}
