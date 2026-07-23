package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class TeamNotFoundException extends DomainException {
    public TeamNotFoundException() {
        super("Setor não encontrado", HttpStatus.NOT_FOUND);
    }
}
