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
	public List<NewsletterNFeInfo> getNfeInfoByExcel(InputStream stream) throws Exception {
		List<NewsletterNFeInfo> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow; i++) {
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				NewsletterNFeInfo info = new NewsletterNFeInfo();
				
				Cell numNota = row.getCell(0);
				if(numNota != null)
					info.setNfeNumber(String.valueOf((int) numNota.getNumericCellValue()));		
				
				Cell dataCompleta = row.getCell(1);
				if(dataCompleta != null &&
						dataCompleta.getCellType() == CellType.NUMERIC &&
						DateUtil.isCellDateFormatted(dataCompleta)) {
					info.setDate(dataCompleta.getDateCellValue()
							.toInstant()
							.atZone(ZoneId.systemDefault())
							.toLocalDate());
					
				}else if (dataCompleta.getCellType() == CellType.STRING) {
					String value = dataCompleta.getStringCellValue().trim()
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
						info.setDate(LocalDate.parse(value, formatter));
					 } catch (Exception e) {
					        LOGGER.info("⚠️ Erro ao converter data da célula: [" + value + "] " + e.getMessage());
					    }
				}
				
				Cell cod = row.getCell(2);
				if(cod != null)
					info.setPartnerCode(String.valueOf((int) cod.getNumericCellValue()));
				
				Cell nome = row.getCell(3);
				if(nome != null)
					info.setPartnerName(nome.getStringCellValue());

//				Cell apelido = row.getCell(4);
//				if(apelido != null)
//					info.setPartnerName(apelido.getStringCellValue());
				
				Cell codProd = row.getCell(5);
				if(codProd != null)
					info.setProductCode(String.valueOf((int) codProd.getNumericCellValue()));
				
				Cell produto = row.getCell(7);
				if(produto != null)
					info.setProductName(produto.getStringCellValue());
				
				Cell qtd = row.getCell(8);
				if(qtd != null && qtd.getCellType() == CellType.NUMERIC)
					info.setQuantity(qtd.getNumericCellValue());
				
				Cell totalComImposto = row.getCell(9);
				if(totalComImposto != null && totalComImposto.getCellType() == CellType.NUMERIC)
					info.setValueWithTaxes(totalComImposto.getNumericCellValue());
					
				list.add(info);
			}
		}
		
		return list;
	}

	@Override
	public List<NewsletterServiceOrders> getServiceOrdersByExcel(InputStream stream) throws Exception {
		List<NewsletterServiceOrders> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow; i++) {
				
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				NewsletterServiceOrders order = new NewsletterServiceOrders();
				
				Cell numOsCell = row.getCell(0);
				if(numOsCell != null)
					order.setServiceOrderNumber(String.valueOf((int) numOsCell.getNumericCellValue()));
				
				Cell codParCell = row.getCell(1);
				if(codParCell != null)
					order.setPartnerCode(String.valueOf((int) codParCell.getNumericCellValue()));
				
				Cell aberturaChamadoCell = row.getCell(3);
				if(aberturaChamadoCell != null && 
						aberturaChamadoCell.getCellType() == CellType.NUMERIC &&
						DateUtil.isCellDateFormatted(aberturaChamadoCell))
					order.setOpeningDate(aberturaChamadoCell.getDateCellValue()
							.toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate());
				
				Cell fechamentoChamadoCell = row.getCell(4);
				if(fechamentoChamadoCell != null &&
						fechamentoChamadoCell.getCellType() == CellType.NUMERIC &&
						DateUtil.isCellDateFormatted(fechamentoChamadoCell))
					order.setClosingDate(fechamentoChamadoCell.getDateCellValue()
							.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				
				Cell diasDeSemanaCell = row.getCell(5);
				if(diasDeSemanaCell != null && diasDeSemanaCell.getCellType() == CellType.NUMERIC)
					order.setDaysOfWeek((int) diasDeSemanaCell.getNumericCellValue());
				
				list.add(order);
			}
		}
		
		return list;
	}

	@Override
	public List<NewsletterExchangedParts> getExchangedPartsByExcel(InputStream stream) throws Exception {
		List<NewsletterExchangedParts> list = new ArrayList<>();
		
		try(XSSFWorkbook workbook = new XSSFWorkbook(stream)){
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			int lastRow = sheet.getLastRowNum();
			
			for(int i = FIRST_DATA_ROW; i < lastRow ; i++) {
				
				Row row = sheet.getRow(i);
				if(row == null) continue;
				
				NewsletterExchangedParts parts = new NewsletterExchangedParts();
				
				@SuppressWarnings("null")
				Cell codParCell = row.getCell(0);
				if(codParCell != null)
					parts.setPartnerCode(String.valueOf((int) codParCell.getNumericCellValue()));
				
				Cell custoTotalCell = row.getCell(2);
				if(custoTotalCell != null && 
						custoTotalCell.getCellType() == CellType.NUMERIC)
					parts.setTotalCost(custoTotalCell.getNumericCellValue());
					
				list.add(parts);
			}
		}
		
		return list;
	}

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

	@Override
	public List<Newsletter> getNewsletterInfoByExcel(InputStream stream){
		List<Newsletter> list = new ArrayList<>();

		try{
			XSSFWorkbook workbook = new XSSFWorkbook(stream);

			XSSFSheet sheet = workbook.getSheetAt(0);

			int lastRow = sheet.getLastRowNum();

			for(int i = 1; i < lastRow; i++){
				Row row  = sheet.getRow(i);
				if(row == null) continue;

				Newsletter newsletter = new Newsletter();

				Optional<Cell> codigoParceiro = Optional.ofNullable(row.getCell(0));
                codigoParceiro.ifPresent(cell -> newsletter.setCodigoCliente(cell.getStringCellValue()));

				Optional<Cell> codigoMatriz = Optional.ofNullable(row.getCell(1));
				codigoMatriz.ifPresent(cell -> newsletter.setMatrizCode(cell.getStringCellValue()));

				Optional<Cell> nomeCliente = Optional.ofNullable(row.getCell(2));
				nomeCliente.ifPresent(cell -> newsletter.setNomeDoCliente(cell.getStringCellValue()));

				Optional<Cell> nomeMatriz = Optional.ofNullable(row.getCell(3));
				nomeMatriz.ifPresent(cell -> newsletter.setMatrizName(cell.getStringCellValue()));

				Optional<Cell> data = Optional.ofNullable(row.getCell(4));
				data.ifPresent(cell -> newsletter.setData(LocalDate.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault())));

				Optional<Cell> mes = Optional.ofNullable(row.getCell(5));
				mes.ifPresent(cell -> newsletter.setMes(cell.getStringCellValue()));

				Optional<Cell> quantidadeProdutos = Optional.ofNullable(row.getCell(6));
				quantidadeProdutos.ifPresent(cell -> newsletter.setQuantidadeDeProdutos(((int) cell.getNumericCellValue())));

				Optional<Cell> quantidadeLitros = Optional.ofNullable(row.getCell(7));
				quantidadeLitros.ifPresent(cell -> newsletter.setQuantidadeDeLitros((cell.getNumericCellValue())));

				Optional<Cell> quantidadeVisitas = Optional.ofNullable(row.getCell(8));
				quantidadeVisitas.ifPresent(cell -> newsletter.setQuantidadeDeVisitas(((int) cell.getNumericCellValue())));

				Optional<Cell> quantidadeNotasEmitidas = Optional.ofNullable(row.getCell(9));
				quantidadeNotasEmitidas.ifPresent(cell -> newsletter.setQuantidadeNotasEmitidas(((int) cell.getNumericCellValue())));

				Optional<Cell> mediaDiasEmAtendimento = Optional.ofNullable(row.getCell(10));
				mediaDiasEmAtendimento.ifPresent(cell -> newsletter.setMediaDiasAtendimento(((int) cell.getNumericCellValue())));

				Optional<Cell> produtoMaisComprado = Optional.ofNullable(row.getCell(11));
				produtoMaisComprado.ifPresent(cell -> newsletter.setProdutoEmDestaque(cell.getStringCellValue()));

				Optional<Cell> totalFaturado = Optional.ofNullable(row.getCell(12));
				totalFaturado.ifPresent(cell -> newsletter.setFaturamentoTotal(cell.getNumericCellValue()));

				Optional<Cell> vlrPecasTrocadas = Optional.ofNullable(row.getCell(13));
				vlrPecasTrocadas.ifPresent(cell -> newsletter.setValorDePecasTrocadas(cell.getNumericCellValue()));

				Optional<Cell> vlrTotalHoras = Optional.ofNullable(row.getCell(14));
				vlrTotalHoras.ifPresent(cell -> newsletter.setValorTotalDeHoras(cell.getNumericCellValue()));

				Optional<Cell> vlrTotalCobradoHoras = Optional.ofNullable(row.getCell(15));
				vlrTotalCobradoHoras.ifPresent(cell -> newsletter.setValorTotalCobradoHoras(cell.getNumericCellValue()));

				Optional<Cell> vlrCobradoMauUso = Optional.ofNullable(row.getCell(17));
				vlrCobradoMauUso.ifPresent(cell -> newsletter.setValorTotalCobradoHorasMauUso(cell.getNumericCellValue()));

				Optional<Cell> vlrTotalHorasMauUso = Optional.ofNullable(row.getCell(18));
				vlrTotalHorasMauUso.ifPresent(cell -> newsletter.setValorTotalCobradoHorasMauUso(cell.getNumericCellValue()));

				Optional<Cell> emailCliente = Optional.ofNullable(row.getCell(19));
				emailCliente.ifPresent(cell -> newsletter.setEmailCliente(cell.getStringCellValue()));

				list.add(newsletter);
			}
		}catch (Exception e) {
			if (e instanceof IOException)
				throw new NewsletterFileNotValidException("Erro ao ler o arquivo: " + e.getMessage() + "\n\nCausado por: " + e.getCause());
		}

		return list;
	}
}
