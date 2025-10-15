package com.proautokimium.api.Infrastructure.services.fuelsupply;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Infrastructure.interfaces.fuelsupply.IFuelSupplyReader;
import com.proautokimium.api.domain.entities.FuelSupply;

@Service
public class FuelSupplyReaderService implements IFuelSupplyReader{
	
	private final int FIRST_DATA_ROW = 1;

	@Override
	public List<FuelSupply> getFuelSuppliesByExcel(InputStream stream) throws Exception {
		List<FuelSupply> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lasRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i <= lasRow; i++) {
				Row row = sheet.getRow(i);
				
				if(row == null) continue;
				
				FuelSupply fuel = new FuelSupply();
				
				Cell driverName = row.getCell(0);
				if(driverName != null)
					fuel.setDriverName(driverName.getStringCellValue());
				
				Cell date = row.getCell(1);
				if(date != null && date.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(date)) {
					fuel.setFuelSupplyDate(date.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				}else if (date.getCellType() == CellType.STRING) {
					String value = date.getStringCellValue().trim()
							.replace("\u00A0", "")
							.replace("\u202F", "")
							.replace(".", "/")
							.replace("-", "/");
					
					try {
						DateTimeFormatter formatter;
						if(value.matches("\\d{4}-\\d{2}-\\d{2}")) {
							formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
						}else if (value.matches("\\d{2}-\\d{2}-\\d{4}")) {
							formatter = DateTimeFormatter.ofPattern("dd/mm/yyyy");
						}else {
							formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
						}
						fuel.setFuelSupplyDate(LocalDate.parse(value, formatter));
					 } catch (Exception e) {
					        System.err.println("⚠️ Erro ao converter data da célula: [" + value + "] " + e.getMessage());
				    }
				}
				
				Cell uf = row.getCell(3);
				if(uf != null)
					fuel.setUf(uf.getStringCellValue());
					
				Cell plate = row.getCell(4);
				if(plate != null)
					fuel.setPlate(plate.getStringCellValue());
				
				Cell actualHodometer = row.getCell(5);
				if(actualHodometer != null && plate.getCellType() == CellType.NUMERIC)
					fuel.setActualHodometer((int) actualHodometer.getNumericCellValue());
				
				Cell fuelType = row.getCell(6);
				if(fuelType != null)
					fuel.setFuelType(fuelType.getStringCellValue());
				
				Cell totalValue = row.getCell(8);
				if(totalValue != null && totalValue.getCellType() == CellType.NUMERIC)
					fuel.setTotalValue(totalValue.getNumericCellValue());
				
				Cell price = row.getCell(9);
				if(price != null && price.getCellType() == CellType.NUMERIC)
					fuel.setPrice(price.getNumericCellValue());
				
				Cell hodometer = row.getCell(11);
				if(hodometer != null && hodometer.getCellType() == CellType.NUMERIC)
					fuel.setDiferenceHodometer((int) hodometer.getNumericCellValue());
				
				Cell averageKilometer = row.getCell(12);
				if(averageKilometer != null && averageKilometer.getCellType() == CellType.NUMERIC)
					fuel.setAverageKm(averageKilometer.getNumericCellValue());			
				list.add(fuel);
			}
		}
		
		return list;
	}
	
}
