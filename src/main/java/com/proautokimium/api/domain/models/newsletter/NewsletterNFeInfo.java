package com.proautokimium.api.domain.models.newsletter;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewsletterNFeInfo {
	private String nfeNumber;
	private LocalDate date;
	private String partnerCode;
	private String partnerName;
	private String productCode;
	private String productName;
	private char unitType;
	public double quantity;
	public double valueWithTaxes;
}
