package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** O desafio venceu, já foi usado, ou não é deste pedido. A saída é pedir outro. */
public class WebAuthnChallengeExpiredException extends DomainException {
    public WebAuthnChallengeExpiredException() {
        super("O pedido da digital venceu. Tente de novo.", HttpStatus.BAD_REQUEST);
    }
}
