package com.proautokimium.api.Infrastructure.exceptions.newsletter;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class NewsletterNullException extends DomainException {
    public NewsletterNullException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
