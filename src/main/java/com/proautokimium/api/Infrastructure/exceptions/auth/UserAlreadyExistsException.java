package com.proautokimium.api.Infrastructure.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
    public UserAlreadyExistsException() {
        super("Usuário já existe", HttpStatus.CONFLICT);
    }
}
