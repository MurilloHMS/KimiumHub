package com.proautokimium.api.Infrastructure.exceptions.secrets;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class SecretNotFoundException extends DomainException {
    public SecretNotFoundException() {
        super("Segredo não encontrado", HttpStatus.NOT_FOUND);
    }
}
