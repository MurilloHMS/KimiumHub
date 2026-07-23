package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PositionLevelNotFoundException extends DomainException {
    public PositionLevelNotFoundException() {
        super("Nível de cargo não encontrado", HttpStatus.NOT_FOUND);
    }
}
