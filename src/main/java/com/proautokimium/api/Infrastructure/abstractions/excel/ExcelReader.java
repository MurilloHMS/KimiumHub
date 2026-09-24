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

	/**
	 * Uma linha vira um objeto.
	 *
	 * @param header o cabecalho da planilha, para quem casa coluna pelo NOME.
	 *               Quem ainda casa por posicao simplesmente ignora
	 */
	protected abstract T mapRow(Row row, SheetHeader header);

	protected  int getFirstDataRow(){
		return 0;
	}

	/**
	 * A linha do cabecalho: a de cima da primeira linha de dados.
	 *
	 * <p>Planilha que comeca a valer na linha 0 nao tem cabecalho, e ai o
	 * {@link SheetHeader} vem vazio.
	 */
	protected int getHeaderRow(){
		return getFirstDataRow() - 1;
	}

	protected String getString(Row row, int cell) {
		return ExcelReaderHelper.returnStringCellValue(row.getCell(cell));
	}

	protected Double getDouble(Row row, int cell) {
		try {
			return ExcelReaderHelper.returnDoubleCellValue(row.getCell(cell));
		} catch (NumberFormatException e) {
			throw naoENumero(row, cell);
		}
	}

	protected Integer getInteger(Row row, int cell) {
		try {
			return ExcelReaderHelper.returnIntegerCellValue(row.getCell(cell));
		} catch (NumberFormatException e) {
			throw naoENumero(row, cell);
		}
	}

	/**
	 * O erro que o {@code Double.parseDouble} dá, reescrito para quem está
	 * olhando a planilha.
	 *
	 * <p>Ele dizia só {@code For input string: "Gasolina Comum"}: nem a linha,
	 * nem a coluna, nem o que era esperado ali. Quem recebe isso não tem como
	 * saber se conserta a célula ou o arquivo inteiro.
	 */
	private IllegalArgumentException naoENumero(Row row, int cell) {
		String valor = ExcelReaderHelper.returnStringCellValue(row.getCell(cell));

		return new IllegalArgumentException(
				"Linha " + (row.getRowNum() + 1) + ", coluna " + letraDaColuna(cell)
						+ ": esperava um número e veio \"" + valor + "\".");
	}

	/** O nome da coluna como o Excel mostra no topo: A, B, ... Z, AA. */
	private static String letraDaColuna(int indice) {
		StringBuilder letras = new StringBuilder();

		for (int n = indice; n >= 0; n = n / 26 - 1) {
			letras.insert(0, (char) ('A' + n % 26));
		}

		return letras.toString();
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

			SheetHeader header = getHeaderRow() >= 0
					? SheetHeader.de(sheet.getRow(getHeaderRow()))
					: SheetHeader.vazio();

			for (int i = getFirstDataRow(); i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				if (row == null) {
					continue;
				}

				// +1 porque o POI conta de 0 e o Excel conta de 1: a linha 0 do
				// POI e a linha 1 da planilha sao a mesma.
				data.add(new ReadRow<>(i + 1, mapRow(row, header)));
			}
		}

		return data;
	}
}
