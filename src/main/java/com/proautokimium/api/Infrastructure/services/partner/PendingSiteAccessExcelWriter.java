package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.PendingSiteAccessRowDTO;
import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

/** A planilha dos pendentes: uma pessoa por linha, para o RH filtrar e cobrar. */
@Component
public class PendingSiteAccessExcelWriter extends ExcelWriter<PendingSiteAccessRowDTO> {

    @Override
    protected String getSheetName() {
        return "Sem acesso ao site";
    }

    @Override
    protected String[] getHeaders() {
        return new String[] { "Código", "Nome", "E-mail", "Empresa", "Departamento / Setor", "Cargo", "Situação" };
    }

    @Override
    protected void writeDataRow(Row row, PendingSiteAccessRowDTO item, Workbook workbook) {
        setCell(row, 0, item.getCode());
        setCell(row, 1, item.getName());
        setCell(row, 2, item.getEmail());
        setCell(row, 3, item.getCompany());
        setCell(row, 4, item.getDepartment());
        setCell(row, 5, item.getPosition());
        setCell(row, 6, item.getDetail());
    }
}
