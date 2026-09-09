package com.proautokimium.api.domain.exceptions.email;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmailInvalidException extends DomainException {
    public EmailInvalidException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
