package com.proautokimium.api.Infrastructure.exceptions.sales;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** Cadastro do checklist recusado: nome repetido, produto já escolhido, item inexistente. */
public class ChecklistRegisterException extends DomainException {
    public ChecklistRegisterException(String message, HttpStatus status) {
        super(message, status);
    }
}
