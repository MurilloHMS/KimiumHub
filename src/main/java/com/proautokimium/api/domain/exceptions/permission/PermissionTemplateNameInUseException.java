package com.proautokimium.api.domain.exceptions.permission;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PermissionTemplateNameInUseException extends DomainException {
    public PermissionTemplateNameInUseException(String name) {
        super("Já existe um modelo chamado \"" + name + "\".", HttpStatus.CONFLICT);
    }
}
