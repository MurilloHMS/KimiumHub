package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CandidaturaAlreadyExistsException extends DomainException {
    public CandidaturaAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
