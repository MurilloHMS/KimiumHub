package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class PerguntaNotFoundExeption extends DomainException {
    public PerguntaNotFoundExeption() {
        super("Pergunta não encontrada", HttpStatus.NOT_FOUND);
    }
}
