package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CandidatoNotFoundException extends DomainException {
    public CandidatoNotFoundException() {
        super("Candidato não encontrado.", HttpStatus.NOT_FOUND);
    }
}
