package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import com.proautokimium.api.domain.entities.MovementInventory;

@Service
public class InventoryProductsExcelWriterService extends ExcelWriter<MovementInventory>{
	
	@Override
	protected byte[] save(List<MovementInventory> list) throws Exception {
		if(list == null || list.isEmpty())
			throw new IllegalArgumentException("Dados obtidos inválidos");
		
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Movements");
		
		CreationHelper ch = workbook.getCreationHelper();
		CellStyle cs = workbook.createCellStyle();
		short dateFormat = ch.createDataFormat().getFormat("dd/MM/yyyy");
		cs.setDataFormat(dateFormat);
		
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("Código Sistema");
		header.createCell(1).setCellValue("Produto");
		header.createCell(2).setCellValue("Estoque do dia");
		header.createCell(3).setCellValue("Data do Estoque");
		
		int rowNum = 1;
		
		for(MovementInventory i : list) {
			Row row = sheet.createRow(rowNum++);
			
			row.createCell(0).setCellValue(i.getProduct().getSystemCode());
			row.createCell(1).setCellValue(i.getProduct().getName());
			row.createCell(2).setCellValue(i.getQuantity());
			Cell dc = row.createCell(3);
			if(i.getMovementDate() != null) {
				dc.setCellValue(i.getMovementDate());
				dc.setCellStyle(cs);
			}
			
		}
		
		for(int i = 0 ; i < 4; i++) {
			sheet.autoSizeColumn(i);
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		workbook.write(bos);
		workbook.close();
		
		return bos.toByteArray();
	}

}
