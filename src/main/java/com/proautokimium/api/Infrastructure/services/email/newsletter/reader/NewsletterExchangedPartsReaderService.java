package com.proautokimium.api.Infrastructure.services.email.newsletter.reader;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
public class NewsletterExchangedPartsReaderService extends ExcelReader<NewsletterExchangedParts> {

    @Override
    protected int getFirstDataRow(){ return 3;}

    @Override
    protected NewsletterExchangedParts mapRow(Row row){
        NewsletterExchangedParts parts = new NewsletterExchangedParts();

        parts.setPartnerCode(getString(row,0));
        parts.setTotalCost(getDouble(row,2));

        return parts;
    }
}
