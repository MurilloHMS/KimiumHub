package com.proautokimium.api.Application.DTOs.email;

import java.util.List;

import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

public record NewsletterData(List<NewsletterNFeInfo> nFeInfos,
		List<NewsletterServiceOrders> orders,
		List<NewsletterTechnicalHours> hours,
		List<NewsletterExchangedParts> parts) {}
