package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** 409, e não o erro de chave única do banco: a mensagem chega à tela. */
public class EmployeeDocumentTypeAlreadyExistsException extends DomainException {
    public EmployeeDocumentTypeAlreadyExistsException() {
        super("Já existe um tipo de documento com esse nome", HttpStatus.CONFLICT);
    }
}