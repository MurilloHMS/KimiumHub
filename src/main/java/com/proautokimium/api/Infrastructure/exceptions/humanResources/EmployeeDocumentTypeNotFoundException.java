package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmployeeDocumentTypeNotFoundException extends DomainException {
    public EmployeeDocumentTypeNotFoundException() {
        super("Tipo de documento não encontrado", HttpStatus.NOT_FOUND);
    }
}