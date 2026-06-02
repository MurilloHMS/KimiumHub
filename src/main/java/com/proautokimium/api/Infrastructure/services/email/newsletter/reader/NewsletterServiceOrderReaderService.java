package com.proautokimium.api.Infrastructure.services.email.newsletter.reader;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
public class NewsletterServiceOrderReaderService extends ExcelReader<NewsletterServiceOrders> {

    @Override
    protected int getFirstDataRow(){ return 3;}

    @Override
    protected NewsletterServiceOrders mapRow(Row row){
        NewsletterServiceOrders orders = new NewsletterServiceOrders();

        orders.setServiceOrderNumber(getString(row, 0));
        orders.setPartnerCode(getString(row, 1));
        orders.setOpeningDate(getDate(row, 3));
        orders.setClosingDate(getDate(row, 4));
        orders.setDaysOfWeek(getInteger(row, 5));

        return orders;
    }
}
