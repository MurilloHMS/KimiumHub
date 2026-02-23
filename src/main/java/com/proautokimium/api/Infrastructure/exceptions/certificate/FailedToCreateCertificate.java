package com.proautokimium.api.Infrastructure.exceptions.certificate;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class FailedToCreateCertificate extends DomainException {
    public FailedToCreateCertificate(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
