package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

import java.math.BigDecimal;

/**
 * Os totais do mês, pela data da despesa — a mesma regra do comprovante, para
 * os números da tela baterem com os do PDF.
 *
 * Enviado conta tudo que foi pedido no mês, inclusive recusado. Aprovado é o
 * que ainda vai ser pago; o que já foi pago fica em Pago (decidido em 2026-09-28).
 *
 * @param month          {@code "2026-09"}
 * @param contestedPending quantos dos pendentes são contestação
 */
public record ReimbursementSummaryDTO(String month, Bucket sent, Bucket pending, Bucket approved, Bucket paid,
                                      long contestedPending) {

    public record Bucket(BigDecimal amount, long count) {}
}
