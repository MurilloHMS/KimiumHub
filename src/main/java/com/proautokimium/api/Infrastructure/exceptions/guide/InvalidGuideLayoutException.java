package com.proautokimium.api.Infrastructure.exceptions.guide;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** O layout não pode ser montado. A mensagem diz onde: qual coluna, qual elemento. */
public class InvalidGuideLayoutException extends DomainException {
    public InvalidGuideLayoutException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
