package com.proautokimium.api.Infrastructure.exceptions.file;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

public class FileStorageException extends InfrastructureException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException() {
        super("Erro ao salvar o arquivo.");
    }
}
