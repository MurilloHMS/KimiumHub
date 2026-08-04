package com.proautokimium.api.Infrastructure.exceptions.file;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class FileNotFoundException extends DomainException {
    public FileNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
    public FileNotFoundException() {
        super("Arquivo não encontrado.", HttpStatus.NOT_FOUND);
    }
}
