package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** A mesma credencial cadastrada duas vezes: o aparelho já estava ativado. */
public class WebAuthnAlreadyRegisteredException extends DomainException {
    public WebAuthnAlreadyRegisteredException() {
        super("A digital deste aparelho já está ativada.", HttpStatus.CONFLICT);
    }
}
