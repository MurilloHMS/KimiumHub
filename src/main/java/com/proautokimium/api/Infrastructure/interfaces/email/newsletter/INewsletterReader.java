package com.proautokimium.api.Infrastructure.interfaces.email.newsletter;

import java.io.InputStream;
import java.util.List;

import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.models.newsletter.*;

public interface INewsletterReader {
	List<NewsletterNFeInfo> getNfeInfoByExcel(InputStream stream) throws Exception;
	List<NewsletterServiceOrders> getServiceOrdersByExcel(InputStream stream) throws Exception;
	List<NewsletterExchangedParts> getExchangedPartsByExcel(InputStream stream) throws Exception;
	List<NewsletterTechnicalHours> getTechnicalHoursByExcel(InputStream stream) throws Exception;
	List<Newsletter> getNewsletterInfoByExcel(InputStream stream);
}
