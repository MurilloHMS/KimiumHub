package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeWriter;
import com.proautokimium.api.domain.models.NfeIcmsInfo;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
}
