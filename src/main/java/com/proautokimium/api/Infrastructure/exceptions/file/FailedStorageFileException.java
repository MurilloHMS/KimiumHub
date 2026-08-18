package com.proautokimium.api.Infrastructure.exceptions.file;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

public class FailedStorageFileException extends InfrastructureException {
    public FailedStorageFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedStorageFileException() {
        super("Ocorreu um erro ao salvar o arquivo");
    }
}
