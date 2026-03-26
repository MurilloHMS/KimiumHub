package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CandidatoAlreadyExistsException extends DomainException {
    public CandidatoAlreadyExistsException() {
        super("Candidato já existe", HttpStatus.CONFLICT);
    }
}
