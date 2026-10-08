package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

/** Falha técnica ao montar o relatório dos pendentes (planilha, template do PDF). 503; o detalhe fica no log. */
public class SiteAccessReportException extends InfrastructureException {
    public SiteAccessReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
