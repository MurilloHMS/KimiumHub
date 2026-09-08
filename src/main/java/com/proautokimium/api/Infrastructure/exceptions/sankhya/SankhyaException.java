package com.proautokimium.api.Infrastructure.exceptions.sankhya;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class SankhyaException extends DomainException {
    public SankhyaException() {
        super("Ocorreu um erro ao consultar o sankhya", HttpStatus.BAD_REQUEST);
    }

    public SankhyaException(String message) {super(message, HttpStatus.BAD_REQUEST);}
}
