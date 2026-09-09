package com.proautokimium.api.Infrastructure.exceptions.newsletter;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class NewsletterPreviaNaoEncontradaException extends DomainException {

    public NewsletterPreviaNaoEncontradaException(String mensagem) {
        super(mensagem, HttpStatus.NOT_FOUND);
    }
}
