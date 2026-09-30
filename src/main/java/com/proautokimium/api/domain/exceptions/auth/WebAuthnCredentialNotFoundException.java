package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Remover um aparelho que não existe — ou que é de outra pessoa. Os dois casos
 * dão 404: responder 403 para "é de outro" confirmaria que o id existe.
 */
public class WebAuthnCredentialNotFoundException extends DomainException {
    public WebAuthnCredentialNotFoundException() {
        super("Aparelho não encontrado.", HttpStatus.NOT_FOUND);
    }
}
