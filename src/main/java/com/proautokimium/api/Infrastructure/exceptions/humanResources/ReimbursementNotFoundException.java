package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ReimbursementNotFoundException extends DomainException {
    public ReimbursementNotFoundException() {
        super("Reembolso não encontrado", HttpStatus.NOT_FOUND);
    }
}
