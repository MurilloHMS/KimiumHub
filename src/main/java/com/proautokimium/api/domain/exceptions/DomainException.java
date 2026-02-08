package com.proautokimium.api.domain.exceptions;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {
    protected DomainException(String message){
        super(message);
    }

    public abstract HttpStatus getStatus();
}
