package com.proautokimium.api.Infrastructure.helpers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ExcelReaderHelper {

    public static String returnStringCellValue(Cell cell){
        if(cell != null && cell.getCellType() == CellType.STRING){
            return cell.getStringCellValue();
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC){
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }

    public static Double returnDoubleCellValue(Cell cell){
        if(cell != null && cell.getCellType() == CellType.NUMERIC){
            return cell.getNumericCellValue();
        }else if (cell != null && cell.getCellType() == CellType.STRING){
            return Double.parseDouble(cell.getStringCellValue());
        }
        return null;
    }

    public static Integer returnIntegerCellValue(Cell cell){
        if(cell != null && cell.getCellType() == CellType.NUMERIC){
            return (int) cell.getNumericCellValue();
        }else if (cell != null && cell.getCellType() == CellType.STRING){
            return Integer.parseInt(cell.getStringCellValue());
        }
        return null;
    }

    public static LocalDate returnLocalDateCellValue(Cell cell){
        if(cell != null &&
                cell.getCellType() == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

        }else if (cell != null && cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim()
                    .replace("\u00A0", " ")
                    .replace("\u202F", " ");

            // "28/08/2026 15:42:56": a entidade guarda s\u00F3 o dia, ent\u00E3o a hora
            // (tudo depois do primeiro espa\u00E7o) \u00E9 descartada antes do parse.
            String data = value.split("\\s+")[0]
                    .replace(".", "/")
                    .replace("-", "/");

            try {
                return LocalDate.parse(data, DateTimeFormatter.ofPattern("d/M/yyyy"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
