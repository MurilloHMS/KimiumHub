package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Uma linha do comprovante de reembolsos, já formatada para o PDF.
 *
 * Classe com getters, e não record: o JRBeanCollectionDataSource lê os campos
 * por getter de JavaBean ({@code getX()}), e o acessor de record ({@code x()})
 * ele não encontra.
 *
 * Datas e valores chegam como texto pronto em pt-BR: formatar no Java é uma
 * regra só, testável, em vez de um {@code pattern} em cada campo do jrxml.
 */
@Getter
@AllArgsConstructor
public class ReimbursementReportRowDTO {
    /** Chave do agrupamento por funcionário (o id). */
    private final String employeeKey;
    private final String employeeName;
    /** "Cód. 1042 · Administrativo" */
    private final String employeeInfo;
    private final String expenseDate;
    private final String category;
    private final String reason;
    private final String requestedAt;
    private final String reviewerName;
    /** Data da análise e, se houver, a observação de quem revisou. */
    private final String reviewDetail;
    /** PENDING, APPROVED, PAID ou REJECTED — o jrxml escolhe a cor por ele. */
    private final String statusKey;
    private final String statusLabel;
    private final String paymentDate;
    /** "A-1", "A-2"… quando o comprovante vai anexado; nulo no relatório de todos. */
    private final String annexCode;
    /** Valor numérico, para o subtotal por funcionário que o Jasper soma. */
    private final BigDecimal amount;
    private final String amountLabel;
}
