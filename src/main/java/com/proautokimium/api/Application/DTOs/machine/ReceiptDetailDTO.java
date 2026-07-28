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
public class ReceiptDetailDTO {
    private String id;
    private String receiptType;
    private String codMatriz;
    private String nomeMatriz;
    private String nomeParceiro;
    private String dueDate;
    private BigDecimal totalAmount;
    private String originalFilename;
}
