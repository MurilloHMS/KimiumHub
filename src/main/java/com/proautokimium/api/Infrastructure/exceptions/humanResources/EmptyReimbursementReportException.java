package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Nenhum reembolso casou com os filtros. Recusar é melhor que entregar um PDF
 * vazio com cabeçalho e assinatura — ele iria para a diretoria parecendo
 * "não houve gasto", quando o filtro é que estava errado.
 */
public class EmptyReimbursementReportException extends DomainException {
    public EmptyReimbursementReportException() {
        super("Nenhuma solicitação de reembolso encontrada com esses filtros", HttpStatus.NOT_FOUND);
    }
}
