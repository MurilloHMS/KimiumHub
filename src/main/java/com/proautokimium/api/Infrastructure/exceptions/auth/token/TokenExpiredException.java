package com.proautokimium.api.Infrastructure.exceptions.auth.token;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends DomainException {
    public TokenExpiredException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
