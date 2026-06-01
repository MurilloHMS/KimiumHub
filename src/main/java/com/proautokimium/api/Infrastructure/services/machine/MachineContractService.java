package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.models.MachineContract;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;


@Service
public class MachineContractService extends ExcelReader<MachineContract> {

    @Override
    protected int getFirstDataRow(){
        return 4;
    }

    @Override
    protected MachineContract mapRow(Row row) {

        MachineContract contract = new MachineContract();

        contract.setNumeroUnico(getString(row, 0));
        contract.setNumeroNota(getString(row, 1));
        contract.setNomeParceiro(getString(row, 2));
        contract.setDocumento(getString(row, 3));
        contract.setCodigoMatriz(getString(row, 4));
        contract.setNomeMatriz(getString(row, 5));
        contract.setDataNegociacao(getDate(row, 6));
        contract.setCodigoProduto(getString(row, 7));
        contract.setDescricaoProduto(getString(row, 8));
        contract.setVlrUnitario(getDouble(row, 9));
        contract.setObservacao(getString(row, 10));
        contract.setNumeroFinanceiro(getString(row, 11));
        contract.setVlrDesdobramento(getDouble(row, 12));
        contract.setEnderecoEntrega(getString(row, 13));

        return contract;
    }
}
