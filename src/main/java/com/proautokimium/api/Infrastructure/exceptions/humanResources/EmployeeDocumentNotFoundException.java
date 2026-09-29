package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmployeeDocumentNotFoundException extends DomainException {
    public EmployeeDocumentNotFoundException() {
        super("Documento não encontrado", HttpStatus.NOT_FOUND);
    }
}
