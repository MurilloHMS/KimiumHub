package com.proautokimium.api.domain.models.newsletter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterTechnicalHours {
	private String partnerCode;
	private double timePerPartner;
	private double totalValuePerPartner;
	private boolean minuse;
	private double minuseHour;
	private double minuseValue;
}
