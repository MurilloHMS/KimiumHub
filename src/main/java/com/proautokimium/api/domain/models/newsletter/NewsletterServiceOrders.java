package com.proautokimium.api.domain.models.newsletter;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterServiceOrders {
	private String serviceOrderNumber;
	private String partnerCode;
	private LocalDateTime openingDate;
	private LocalDateTime closingDate;
	private int daysOfWeek;
}
