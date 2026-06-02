package com.proautokimium.api.Infrastructure.services.email.newsletter.reader;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
public class NewsletterInfoReaderService extends ExcelReader<NewsletterNFeInfo> {

    @Override
    protected int getFirstDataRow(){ return 3;}

    @Override
    protected NewsletterNFeInfo mapRow(Row row){
        NewsletterNFeInfo info = new NewsletterNFeInfo();

        info.setNfeNumber(getString(row, 0));
        info.setDate(getDate(row, 1));
        info.setPartnerCode(getString(row, 2));
        info.setPartnerName(getString(row, 3));
        info.setProductCode(getString(row, 5));
        info.setProductName(getString(row,7));
        info.setQuantity(getDouble(row, 8));
        info.setValueWithTaxes(getDouble(row, 9));

        return info;
    }
}
