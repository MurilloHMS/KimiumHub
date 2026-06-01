package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelWriter;
import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class InventoryProductsExcelWriterService extends ExcelWriter<MovementInventory> {

	@Override
	protected String getSheetName() {
		return "Movements";
	}

	@Override
	protected String[] getHeaders() {
		return new String[]{
				"Código Sistema",
				"Produto",
				"Estoque do dia",
				"Data do Estoque"
		};
	}

	@Override
	public byte[] write(List<MovementInventory> list) throws Exception {
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Dados obtidos inválidos");
		}

		try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
		     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet(getSheetName());
			writeHeader(sheet, workbook);

			CellStyle dateStyle = createDateStyle(workbook, "dd/MM/yyyy");

			int rowNum = getFirstDataRowIndex();
			for (MovementInventory item : list) {
				Row row = sheet.createRow(rowNum++);

				setCell(row, 0, item.getProduct() != null ? item.getProduct().getSystemCode() : null);
				setCell(row, 1, item.getProduct() != null ? item.getProduct().getName() : null);
				setCell(row, 2, item.getQuantity());
				setCell(row, 3, item.getMovementDate(), dateStyle);
			}

			finishSheet(sheet);
			workbook.write(bos);
			return bos.toByteArray();
		}
	}

	@Override
	protected void writeDataRow(Row row, MovementInventory item, Workbook workbook) {
	}
}