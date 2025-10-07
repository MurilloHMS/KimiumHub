package com.proautokimium.api.Infrastructure.interfaces.email.newsletter;

import java.util.List;

import com.proautokimium.api.Application.DTOs.email.NewsletterData;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.models.Newsletter;

public interface INewsletterBuilder {
	List<Newsletter> buildNewsletters(NewsletterData data, List<Customer> customers);
}
