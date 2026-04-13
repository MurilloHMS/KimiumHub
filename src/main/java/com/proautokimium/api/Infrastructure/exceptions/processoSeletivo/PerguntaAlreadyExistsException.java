package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PerguntaAlreadyExistsException extends DomainException {
    public PerguntaAlreadyExistsException() {
        super("Esta pergunta já existe", HttpStatus.CONFLICT);
    }
}
