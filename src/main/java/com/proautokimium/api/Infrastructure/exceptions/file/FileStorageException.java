package com.proautokimium.api.Infrastructure.exceptions.file;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class FileStorageException extends DomainException {
    public FileStorageException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public FileStorageException() {
        super("Erro ao salvar o arquivo.", HttpStatus.FORBIDDEN);
    }
}
