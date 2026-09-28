package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class NoReportRecipientException extends DomainException {
    public NoReportRecipientException() {
        super("Cadastre pelo menos um e-mail do RH para receber os relatórios", HttpStatus.CONFLICT);
    }
}
