package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
    public UserNotFoundException() {
        super("Usuário não encontrado", HttpStatus.NOT_FOUND);
    }
}
