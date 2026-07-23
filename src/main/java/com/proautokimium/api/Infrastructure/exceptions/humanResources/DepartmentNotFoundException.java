package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class DepartmentNotFoundException extends DomainException {
    public DepartmentNotFoundException() {
        super("Departamento não encontrado", HttpStatus.NOT_FOUND);
    }
}
