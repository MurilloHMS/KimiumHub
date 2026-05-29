package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.Infrastructure.exceptions.machine.MachineContractErrorException;
import com.proautokimium.api.Infrastructure.helpers.ExcelReaderHelper;
import com.proautokimium.api.domain.models.MachineContract;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class MachineContractService extends ExcelReader<MachineContract> {

    private final int FIRST_DATA_ROW = 4;

    @Override
    protected List<MachineContract> getDataByExcel(InputStream stream) throws Exception {

        List<MachineContract> contracts = new ArrayList<>();

        try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
            XSSFSheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for(int i = FIRST_DATA_ROW; i < lastRow; i++){
                Row row = sheet.getRow(i);
                if(row == null) continue;

                MachineContract contract = new MachineContract();

                Cell numeroUnicoCell = row.getCell(0);
                contract.setNumeroUnico(ExcelReaderHelper.returnStringCellValue(numeroUnicoCell));

                Cell numeroNotaCell = row.getCell(1);
                contract.setNumeroNota(ExcelReaderHelper.returnStringCellValue(numeroNotaCell));

                Cell nomeParceiroCell = row.getCell(2);
                contract.setNomeParceiro(ExcelReaderHelper.returnStringCellValue(nomeParceiroCell));

                Cell documentoCell = row.getCell(3);
                contract.setDocumento(ExcelReaderHelper.returnStringCellValue(documentoCell));

                Cell codigoMatrizCell = row.getCell(4);
                contract.setCodigoMatriz(ExcelReaderHelper.returnStringCellValue(codigoMatrizCell));

                Cell nomeMatrizCell = row.getCell(5);
                contract.setNomeMatriz(ExcelReaderHelper.returnStringCellValue(nomeMatrizCell));

                Cell dataNegociacaoCell = row.getCell(6);
                contract.setDataNegociacao(ExcelReaderHelper.returnLocalDateCellValue(dataNegociacaoCell));

                Cell codigoProdutoCell = row.getCell(7);
                contract.setCodigoProduto(ExcelReaderHelper.returnStringCellValue(codigoProdutoCell));

                Cell nomeProdutoCell = row.getCell(8);
                contract.setDescricaoProduto(ExcelReaderHelper.returnStringCellValue(nomeProdutoCell));

                Cell valorUnitarioCell = row.getCell(9);
                contract.setVlrUnitario(ExcelReaderHelper.returnDoubleCellValue(valorUnitarioCell));

                Cell observacaoCell = row.getCell(10);
                contract.setObservacao(ExcelReaderHelper.returnStringCellValue(observacaoCell));

                Cell numeroFinanceiro = row.getCell(11);
                contract.setNumeroFinanceiro(ExcelReaderHelper.returnStringCellValue(numeroFinanceiro));

                Cell valorDesdobramento = row.getCell(12);
                contract.setVlrDesdobramento(ExcelReaderHelper.returnDoubleCellValue(valorDesdobramento));

                Cell enderecoEntrega = row.getCell(13);
                contract.setEnderecoEntrega(ExcelReaderHelper.returnStringCellValue(enderecoEntrega));

                contracts.add(contract);
            }
        }catch (Exception e){
            throw new MachineContractErrorException("Ocorreu um erro ao coletar os dados da planilha: " + e.getMessage());
        }

        return contracts;
    }
}
