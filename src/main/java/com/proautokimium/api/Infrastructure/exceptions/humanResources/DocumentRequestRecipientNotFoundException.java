package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class DocumentRequestRecipientNotFoundException extends DomainException {
    public DocumentRequestRecipientNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public DocumentRequestRecipientNotFoundException() { super("Resposta não encontrada", HttpStatus.NOT_FOUND);}
}
