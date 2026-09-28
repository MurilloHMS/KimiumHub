package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

/** Nenhum destinatário recebeu. 503; o erro do SMTP fica no log, não na tela. */
public class ReportEmailFailedException extends InfrastructureException {
    public ReportEmailFailedException(String message) {
        super(message);
    }
}
