package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ReportRecipientNotFoundException extends DomainException {
    public ReportRecipientNotFoundException() {
        super("Destinatário não encontrado", HttpStatus.NOT_FOUND);
    }
}
