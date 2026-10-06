package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class DocumentRequestFileNotFoundException extends DomainException {
    public DocumentRequestFileNotFoundException() { super("Arquivo não encontrado", HttpStatus.NOT_FOUND);}
}
