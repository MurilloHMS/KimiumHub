package com.proautokimium.api.domain.exceptions.profile;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends DomainException {
    public ProfileNotFoundException() {
        super("Profile não encontrado", HttpStatus.NOT_FOUND);
    }

    public ProfileNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
