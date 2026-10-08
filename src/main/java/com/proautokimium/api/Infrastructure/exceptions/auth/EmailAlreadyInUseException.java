package com.proautokimium.api.Infrastructure.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O e-mail já pertence a outra conta.
 *
 * Existe para a resposta ser 409 com uma frase, e não a violação do índice
 * único estourando como 500 — que era o que o cadastro fazia até 2026-10-08.
 */
public class EmailAlreadyInUseException extends DomainException {
    public EmailAlreadyInUseException() {
        super("Este e-mail já é usado por outra conta.", HttpStatus.CONFLICT);
    }
}
