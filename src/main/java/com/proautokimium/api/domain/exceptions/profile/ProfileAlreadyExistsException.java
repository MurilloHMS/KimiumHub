package com.proautokimium.api.domain.exceptions.profile;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ProfileAlreadyExistsException extends DomainException {
    public ProfileAlreadyExistsException() {
        super("Você já possui um perfil", HttpStatus.CONFLICT);
    }
}
