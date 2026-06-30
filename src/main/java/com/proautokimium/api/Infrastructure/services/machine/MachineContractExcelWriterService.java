package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import com.proautokimium.api.domain.models.MachineContract;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

@Service
public class MachineContractExcelWriterService extends ExcelWriter<MachineContract> {

    @Override
    protected String getSheetName() {
        return "Machine";
    }

    @Override
    protected int getHeaderRowIndex() {
        return 1;
    }

    @Override
    protected String[] getHeaders() {
        return new String[] {
                "Número Único",
                "Número Nota",
                "Nome Parceiro",
                "Documento",
                "Código Matriz",
                "Nome Matriz",
                "Data Negociação",
                "Código Produto",
                "Descrição Produto",
                "Valor Unitário",
                "Observação",
                "Número Financeiro",
                "Valor Desdobramento",
                "Endereço Entrega"
        };
    }

    @Override
    protected void writeDataRow(Row row, MachineContract item, Workbook workbook) {
        setCell(row, 0, item.getNumeroUnico());
        setCell(row, 1, item.getNumeroNota());
        setCell(row, 2, item.getNomeParceiro());
        setCell(row, 3, item.getDocumento());
        setCell(row, 4, item.getCodigoMatriz());
        setCell(row, 5, item.getNomeMatriz());
        setCell(row, 7, item.getCodigoProduto());
        setCell(row, 8, item.getDescricaoProduto());
        setCell(row, 9, item.getVlrUnitario());
        setCell(row, 10, item.getObservacao());
        setCell(row, 11, item.getNumeroFinanceiro());
        setCell(row, 12, item.getVlrDesdobramento());
        setCell(row, 13, item.getEnderecoEntrega());

        if (item.getDataNegociacao() != null) {
            setCell(row, 6, item.getDataNegociacao().atStartOfDay(), createDateStyle(workbook, "dd/MM/yyyy"));
        }
    }
}