package com.proautokimium.api.Infrastructure.exceptions.secrets;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class SecretExpiredException extends DomainException {
    public SecretExpiredException() {
        super("Segredo já foi visualizado", HttpStatus.valueOf(410));
    }
}
