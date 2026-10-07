package com.proautokimium.api.Infrastructure.exceptions;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** E-mail da fila ou remetente que não existe. */
public class EmailRecordNotFoundException extends DomainException {
    public EmailRecordNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
