package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

/** Falha técnica ao montar o PDF (template, fonte, anexo corrompido). 503; o detalhe fica no log. */
public class ReimbursementReportException extends InfrastructureException {
    public ReimbursementReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
