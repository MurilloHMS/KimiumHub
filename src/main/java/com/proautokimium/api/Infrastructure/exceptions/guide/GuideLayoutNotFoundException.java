package com.proautokimium.api.Infrastructure.exceptions.guide;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class GuideLayoutNotFoundException extends DomainException {
    public GuideLayoutNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
