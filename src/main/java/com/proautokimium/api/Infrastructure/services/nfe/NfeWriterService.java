package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeWriter;
import com.proautokimium.api.domain.models.NfeDataInfo;
import com.proautokimium.api.domain.models.NfeIcmsInfo;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class NfeWriterService implements INfeWriter {
    @Override
    public byte[] saveIcmsData(List<NfeIcmsInfo> icmsList) throws Exception {
        if(icmsList == null || icmsList.isEmpty()){
            throw new IllegalArgumentException("Dados ICMS inválidos");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("ICMS");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Número da NFe");
        header.createCell(1).setCellValue("Valor do ICMS");
        header.createCell(2).setCellValue("Valor do PIS");
        header.createCell(3).setCellValue("Valor do COFINS");

        int rowNum = 1;

        for(NfeIcmsInfo info : icmsList){
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(info.getNNF());
            row.createCell(1).setCellValue(info.getVIcms());
            row.createCell(2).setCellValue(info.getVPis());
            row.createCell(3).setCellValue(info.getVCofins());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        return bos.toByteArray();

    }

    @Override
    public byte[] saveNfeData(List<NfeDataInfo> nfeList) throws Exception {
        if(nfeList == null || nfeList.isEmpty()){
            throw new IllegalArgumentException("Dados NFe inválidos");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dados NFe");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Fornecedor");
        header.createCell(1).setCellValue("Número NFe");
        header.createCell(2).setCellValue("Data Emissão");
        header.createCell(3).setCellValue("Produto");
        header.createCell(4).setCellValue("Valor Unitário");
        header.createCell(5).setCellValue("Valor Total");
        header.createCell(6).setCellValue("CFOP");

        int rowNum = 1;

        for(NfeDataInfo info : nfeList){
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(info.getPartner());
            row.createCell(1).setCellValue(info.getNfeNum());

            CreationHelper createHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
            Cell dateCell = row.createCell(2);
            dateCell.setCellValue(info.getNfeDate());
            dateCell.setCellStyle(dateStyle);

            row.createCell(3).setCellValue(info.getProduct());
            row.createCell(4).setCellValue(info.getUnitValue());
            row.createCell(5).setCellValue(info.getTotalValue());
            row.createCell(6).setCellValue(info.getCfop());
        }

        for(int i = 0; i <= 6; i++){
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        return bos.toByteArray();
    }
}
