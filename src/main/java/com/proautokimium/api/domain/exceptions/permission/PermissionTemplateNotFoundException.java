package com.proautokimium.api.domain.exceptions.permission;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PermissionTemplateNotFoundException extends DomainException {
    public PermissionTemplateNotFoundException() {
        super("Modelo de permissão não encontrado.", HttpStatus.NOT_FOUND);
    }
}
