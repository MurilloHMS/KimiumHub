package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PositionNotFoundException extends DomainException {
    public PositionNotFoundException() {
        super("Cargo não encontrado", HttpStatus.NOT_FOUND);
    }
}
