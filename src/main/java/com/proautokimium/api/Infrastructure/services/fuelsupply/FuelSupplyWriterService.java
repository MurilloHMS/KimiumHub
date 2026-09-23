package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import com.proautokimium.api.domain.entities.FuelSupply;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

@Service
public class FuelSupplyWriterService extends ExcelWriter<FuelSupply> {
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

    // todo: finish export method to include data
    @Override
    protected void writeDataRow(Row row, FuelSupply item, Workbook workbook) {

    }
}
