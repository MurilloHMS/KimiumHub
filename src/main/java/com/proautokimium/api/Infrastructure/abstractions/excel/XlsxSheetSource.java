package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class XlsxSheetSource  implements SheetSource {
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.endsWith(".xlsx");
    }

    @Override
    public List<SheetRow> read(InputStream stream) throws Exception {
        List<SheetRow> rows = new ArrayList<>();

        try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
            XSSFSheet sheet = workbook.getSheetAt(0);
            for(int i = 0; i <= sheet.getLastRowNum(); i++){
                Row row  = sheet.getRow(i);
                if(row != null){
                    rows.add(new PoiSheetRow(row));
                }
            }
        }
        return rows;
    }
}
