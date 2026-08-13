package com.proautokimium.api.Infrastructure.abstractions.excel;

import com.proautokimium.api.Infrastructure.helpers.ExcelReaderHelper;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;

public final class PoiSheetRow implements SheetRow {

    private final Row row;

    public PoiSheetRow(Row row) {
        this.row = row;
    }

    @Override
    public String string(int index){
        String value = ExcelReaderHelper.returnStringCellValue(row.getCell(index));
        if (value == null) { return null; }
        return value.isBlank() ?null : value.trim();
    }

    @Override
    public double number(int index) {
        Double value = ExcelReaderHelper.returnDoubleCellValue(row.getCell(index));
        return value == null ? 0.0 : value;
    }

    @Override
    public int integer(int index) {
        Integer value = ExcelReaderHelper.returnIntegerCellValue(row.getCell(index));
        return value == null ? 0 : value;
    }

    @Override
    public LocalDate date(int index) {
        return ExcelReaderHelper.returnLocalDateCellValue(row.getCell(index));
    }
}
