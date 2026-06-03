package com.proautokimium.api.Infrastructure.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CredentialsIncorrectException extends DomainException {
    public CredentialsIncorrectException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
