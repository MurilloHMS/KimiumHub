package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CompanyNotFoundException extends DomainException {
    public CompanyNotFoundException() {
        super("Empresa não encontrada", HttpStatus.NOT_FOUND);
    }
}
