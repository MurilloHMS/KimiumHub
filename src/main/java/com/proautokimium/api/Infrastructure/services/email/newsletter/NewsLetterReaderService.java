package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterReader;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

public class NewsLetterReaderService implements INewsletterReader{
	
	private final int FIRST_DATA_ROW = 3;

	@Override
	public List<NewsletterNFeInfo> getNfeInfoByExcel(InputStream stream) throws Exception {
		List<NewsletterNFeInfo> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i <= lastRow; i++) {
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				NewsletterNFeInfo info = new NewsletterNFeInfo();
				
				Cell numNota = row.getCell(0);
				if(numNota != null)
					info.setNfeNumber(numNota.getStringCellValue());		
				
				Cell dataCompleta = row.getCell(1);
				if(dataCompleta != null &&
						dataCompleta.getCellType() == CellType.NUMERIC &&
						DateUtil.isCellDateFormatted(dataCompleta))
					info.setDate(dataCompleta.getDateCellValue()
							.toInstant()
							.atZone(ZoneId.systemDefault())
							.toLocalDate());
				
				Cell cod = row.getCell(2);
				if(cod != null)
					info.setPartnerCode(cod.getStringCellValue());
				
				Cell nome = row.getCell(3);
				if(nome != null)
					info.setPartnerName(nome.getStringCellValue());
				
				Cell apelido = row.getCell(4);
				if(apelido != null)
					info.setPartnerName(apelido.getStringCellValue());
				
				Cell codProd = row.getCell(5);
				if(codProd != null)
					info.setProductCode(codProd.getStringCellValue());
				
				Cell produto = row.getCell(7);
				if(produto != null)
					info.setProductName(produto.getStringCellValue());
				
				Cell qtd = row.getCell(9);
				if(qtd != null && qtd.getCellType() == CellType.NUMERIC)
					info.setQuantity(qtd.getNumericCellValue());
				
				Cell totalComImposto = row.getCell(10);
				if(totalComImposto != null && totalComImposto.getCellType() == CellType.NUMERIC)
					info.setValueWithTaxes(totalComImposto.getNumericCellValue());
					
				list.add(info);
			}
		}
		
		return list;
	}

	@Override
	public List<NewsletterServiceOrders> getServiceOrdersByExcel(InputStream stream) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NewsletterExchangedParts> getExchangedPartsByExcel(InputStream stream) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NewsletterTechnicalHours> getTechnicalHoursByExcel(InputStream stream) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
}
