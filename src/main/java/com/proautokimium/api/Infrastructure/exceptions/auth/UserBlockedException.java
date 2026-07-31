package com.proautokimium.api.Infrastructure.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class UserBlockedException extends DomainException {
    public UserBlockedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public UserBlockedException(){
        super("Acesso negado! Verifique seu acesso com o RH da empresa", HttpStatus.FORBIDDEN);
    }
}
