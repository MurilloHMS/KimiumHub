package com.proautokimium.api.Infrastructure.exceptions.newsletter;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class NewsletterFileNotValidException extends DomainException {
    public NewsletterFileNotValidException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
