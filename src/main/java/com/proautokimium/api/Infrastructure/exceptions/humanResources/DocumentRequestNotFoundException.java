package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class DocumentRequestNotFoundException extends DomainException {
    public DocumentRequestNotFoundException() {
        super("Solicitação não encontrada", HttpStatus.NOT_FOUND);
    }
}
