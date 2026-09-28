package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ReportRecipientAlreadyExistsException extends DomainException {
    public ReportRecipientAlreadyExistsException() {
        super("Este e-mail já recebe os relatórios do RH", HttpStatus.CONFLICT);
    }
}
