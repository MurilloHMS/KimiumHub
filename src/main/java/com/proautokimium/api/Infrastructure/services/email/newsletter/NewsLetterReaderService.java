package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterFileNotValidException;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.apache.poi.sl.draw.geom.GuideIf;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.binary.XSSFBParseException;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.security.config.annotation.web.oauth2.resourceserver.OpaqueTokenDsl;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterReader;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

@Service
public class NewsLetterReaderService implements INewsletterReader{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NewsLetterReaderService.class);
	private final int FIRST_DATA_ROW = 3;

	@Override
	public List<NewsletterTechnicalHours> getTechnicalHoursByExcel(InputStream stream) throws Exception {
		List<NewsletterTechnicalHours> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow; i++) {
				
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				NewsletterTechnicalHours hours = new NewsletterTechnicalHours();
				
				@SuppressWarnings("null")
				Cell codParCell = row.getCell(0);
				if(codParCell != null)
					hours.setPartnerCode(String.valueOf((int) codParCell.getNumericCellValue()));
				
				Cell horasTotais = row.getCell(1);
				if(horasTotais != null && horasTotais.getCellType() == CellType.NUMERIC)
					hours.setTimePerPartner(horasTotais.getNumericCellValue());
				
				Cell custoTotalCell = row.getCell(2);
				if(custoTotalCell != null && custoTotalCell.getCellType() == CellType.NUMERIC)
					hours.setTotalValuePerPartner(custoTotalCell.getNumericCellValue());

                Cell mauUsoCell = row.getCell(3);
                boolean isMinuse = false;

                if (mauUsoCell != null && mauUsoCell.getCellType() == CellType.STRING) {
                    isMinuse = mauUsoCell
                            .getStringCellValue()
                            .trim()
                            .equalsIgnoreCase("Sim");
                }

                hours.setMinuse(isMinuse);
                if (horasTotais != null && horasTotais.getCellType() == CellType.NUMERIC &&
                        custoTotalCell != null && custoTotalCell.getCellType() == CellType.NUMERIC) {

                    if (isMinuse) {
                        hours.setMinuseHour(horasTotais.getNumericCellValue());
                        hours.setMinuseValue(custoTotalCell.getNumericCellValue());
                    } else {
                        hours.setTimePerPartner(horasTotais.getNumericCellValue());
                        hours.setTotalValuePerPartner(custoTotalCell.getNumericCellValue());
                    }
                }

                list.add(hours);

			}
		}
		
		return list;
	}
}
