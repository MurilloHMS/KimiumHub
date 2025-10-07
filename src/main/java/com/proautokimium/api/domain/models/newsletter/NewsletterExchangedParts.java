package com.proautokimium.api.domain.models.newsletter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterExchangedParts {
	private String partnerCode;
	private double totalCost;
}
