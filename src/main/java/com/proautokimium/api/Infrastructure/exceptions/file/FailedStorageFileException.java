package com.proautokimium.api.Infrastructure.exceptions.file;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class FailedStorageFileException extends DomainException {
    public FailedStorageFileException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public FailedStorageFileException() {
        super("Ocorreu um erro ao salvar o arquivo", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
