package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.entities.ProductInventory;

@Service
public class InventoryProductExcelReaderService extends ExcelReader<ProductInventory>{

	private final int FIRST_DATA_ROW = 1;
	
	@Override
	protected List<ProductInventory> getDataByExcel(InputStream stream) throws Exception {
		List<ProductInventory> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow; i++) {
				
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				ProductInventory product = new ProductInventory();
				
				Cell codProduct = row.getCell(0);
				if(codProduct != null && codProduct.getCellType() == CellType.NUMERIC)
					product.setSystemCode(String.valueOf(codProduct.getNumericCellValue()));
				
				Cell description = row.getCell(1);
				if(description != null)
					product.setName(description.getStringCellValue());
				
				product.setActive(true);
				product.setMinimumStock(0);
				
				list.add(product);
			}
			return list;
		}
	}

}
