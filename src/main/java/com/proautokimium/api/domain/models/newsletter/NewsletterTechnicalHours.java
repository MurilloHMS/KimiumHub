package com.proautokimium.api.domain.models.newsletter;

import java.time.LocalTime;

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
	private LocalTime timePerPartner;
	private double totalValuePerPartner;
}
