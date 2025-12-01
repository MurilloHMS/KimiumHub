package com.proautokimium.api.Infrastructure.services.partner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Infrastructure.interfaces.partner.IPartnerReader;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.valueObjects.Email;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

@Service
public class PartnerReaderService implements IPartnerReader{
	
	private final int FIRST_DATA_ROW = 2;

	@Override
	public List<Customer> getCustomersByExcel(InputStream stream) throws Exception {
		List<Customer> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow; i++) {
				
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				Customer customer = new Customer();
				
				Cell codParCell = row.getCell(0);
				if(codParCell != null && codParCell.getCellType() == CellType.NUMERIC)
					customer.setCodParceiro(String.valueOf((int) codParCell.getNumericCellValue()));
				
				Cell codMatrizCell = row.getCell(1);
				if(codMatrizCell != null && codMatrizCell.getCellType() == CellType.NUMERIC)
					customer.setCodigoMatriz(String.valueOf((int)codMatrizCell.getNumericCellValue()));
				
				Cell razaoSocialCell = row.getCell(3);
				if(razaoSocialCell != null)
					customer.setName(razaoSocialCell.getStringCellValue());
				
				Cell emailCell = row.getCell(4);
				if(emailCell != null)
					customer.setEmail(new Email(emailCell.getStringCellValue()));
				
				list.add(customer);
			}
			return list;
		}
	}

}
