package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class VagaAlreadyExistsException extends DomainException {
    public VagaAlreadyExistsException() {
        super ("Já existe uma vaga cadastrada com essas condições", HttpStatus.CONFLICT);
    }
}
