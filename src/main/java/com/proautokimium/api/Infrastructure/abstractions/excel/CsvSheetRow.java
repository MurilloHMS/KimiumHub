package com.proautokimium.api.Infrastructure.abstractions.excel;

import java.time.LocalDate;

public final class CsvSheetRow implements SheetRow {

    private final String[] cells;

    public CsvSheetRow(String[] cells) {
        this.cells = cells;
    }

    @Override
    public String string(int index) {
        if(index >= cells.length) {return null;}

        String value = cells[index].trim();
        return value.isEmpty() ? null : value;
    }

    @Override
    public double number(int index) {
        String value = string(index);
        if(value == null) { return 0.0; }

        try{
            return Double.parseDouble(value.replace(",", "."));
        }catch (NumberFormatException e){
            return 0.0;
        }
    }

    @Override
    public int integer(int index) {
        return (int) number(index);
    }

    @Override
    public LocalDate date(int index) {
        String value = string(index);
        if(value == null) { return null; }

        try{
            return LocalDate.parse(value);
        }catch (Exception e){
            return null;
        }
    }


}
