package com.proautokimium.api.Infrastructure.abstractions.excel;

import com.proautokimium.api.Infrastructure.helpers.ExcelReaderHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class ExcelReader<T> {

	protected abstract T mapRow(Row row);

	protected  int getFirstDataRow(){
		return 0;
	}

	protected String getString(Row row, int cell) {
		return ExcelReaderHelper.returnStringCellValue(row.getCell(cell));
	}

	protected Double getDouble(Row row, int cell) {
		return ExcelReaderHelper.returnDoubleCellValue(row.getCell(cell));
	}

	protected Integer getInteger(Row row, int cell) {
		return ExcelReaderHelper.returnIntegerCellValue(row.getCell(cell));
	}

	protected LocalDate getDate(Row row, int cell) {
		return ExcelReaderHelper.returnLocalDateCellValue(row.getCell(cell));
	}

	public List<T> getDataByExcel(InputStream stream) throws Exception {
		return readRows(stream).stream().map(ReadRow::valor).toList();
	}

	/**
	 * O mesmo percurso, guardando de que linha cada objeto saiu.
	 *
	 * <p>A leitura é uma só — o {@code getDataByExcel} é esta aqui com o número
	 * jogado fora. Duas travessias separadas divergiriam no dia em que uma
	 * ganhasse uma regra que a outra não tem.
	 */
	public List<ReadRow<T>> readRows(InputStream stream) throws Exception {

		List<ReadRow<T>> data = new ArrayList<>();

		try (XSSFWorkbook workbook = new XSSFWorkbook(stream)) {

			XSSFSheet sheet = workbook.getSheetAt(0);

			for (int i = getFirstDataRow(); i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				if (row == null) {
					continue;
				}

				// +1 porque o POI conta de 0 e o Excel conta de 1: a linha 0 do
				// POI e a linha 1 da planilha sao a mesma.
				data.add(new ReadRow<>(i + 1, mapRow(row)));
			}
		}

		return data;
	}
}
