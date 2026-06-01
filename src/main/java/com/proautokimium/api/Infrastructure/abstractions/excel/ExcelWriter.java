package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

public abstract class ExcelWriter<T> {

	protected abstract String getSheetName();

	protected abstract String[] getHeaders();

	protected abstract void writeDataRow(Row row, T item, Workbook workbook);

	protected int getHeaderRowIndex() {
		return 0;
	}

	protected int getFirstDataRowIndex() {
		return getHeaderRowIndex() + 1;
	}

	public byte[] write(List<T> list) throws Exception {
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Dados obtidos inválidos");
		}

		try (Workbook workbook = new XSSFWorkbook();
		     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet(getSheetName());

			writeHeader(sheet, workbook);

			int rowNum = getFirstDataRowIndex();
			for (T item : list) {
				Row row = sheet.createRow(rowNum++);
				writeDataRow(row, item, workbook);
			}

			finishSheet(sheet);

			workbook.write(bos);
			return bos.toByteArray();
		}
	}

	public byte[] writeTemplate() throws Exception {
		try (Workbook workbook = new XSSFWorkbook();
		     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet(getSheetName());

			writeHeader(sheet, workbook);
			finishSheet(sheet);

			workbook.write(bos);
			return bos.toByteArray();
		}
	}

	protected void writeHeader(Sheet sheet, Workbook workbook) {
		Row header = sheet.createRow(getHeaderRowIndex());
		String[] headers = getHeaders();

		CellStyle headerStyle = createHeaderStyle(workbook);

		for (int i = 0; i < headers.length; i++) {
			Cell cell = header.createCell(i);
			cell.setCellValue(headers[i]);
			if (headerStyle != null) {
				cell.setCellStyle(headerStyle);
			}
		}
	}

	protected CellStyle createHeaderStyle(Workbook workbook) {
		Font font = workbook.createFont();
		font.setBold(true);

		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		return style;
	}

	protected void finishSheet(Sheet sheet) {
		sheet.createFreezePane(0, getFirstDataRowIndex());

		for (int i = 0; i < getHeaders().length; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	protected CellStyle createDateStyle(Workbook workbook, String pattern) {
		CreationHelper creationHelper = workbook.getCreationHelper();
		CellStyle style = workbook.createCellStyle();
		style.setDataFormat(creationHelper.createDataFormat().getFormat(pattern));
		return style;
	}

	protected void setCell(Row row, int column, String value) {
		row.createCell(column).setCellValue(value != null ? value : "");
	}

	protected void setCell(Row row, int column, Integer value) {
		if (value != null) {
			row.createCell(column).setCellValue(value);
		}
	}

	protected void setCell(Row row, int column, Double value) {
		if (value != null) {
			row.createCell(column).setCellValue(value);
		}
	}

	protected void setCell(Row row, int column, LocalDateTime value, CellStyle style) {
		if (value != null) {
			Cell cell = row.createCell(column);
			cell.setCellValue(value);
			if (style != null) {
				cell.setCellStyle(style);
			}
		}
	}
}