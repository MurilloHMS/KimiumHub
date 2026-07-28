package com.proautokimium.api.Application.DTOs.machine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptBatchSummaryDTO {
    private String id;
    private String referenceMonth;
    private int referenceYear;
    private String generatedAt;
    private BigDecimal totalAmount;
    private int receiptCount;
    private String sourceFilename;
}
