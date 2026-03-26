package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class VagaNotFoundException extends DomainException {
    public VagaNotFoundException() {
        super("A vaga informada, Não existe no sistema", HttpStatus.NOT_FOUND);
    }
}
