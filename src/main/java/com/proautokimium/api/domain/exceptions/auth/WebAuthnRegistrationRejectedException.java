package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O cadastro da digital não passou na verificação.
 *
 * <p>{@code 400}, e não o {@code 401} do login: quem cadastra já está logado, e
 * o interceptor do site trata {@code 401} como sessão caída — a pessoa seria
 * deslogada por um cadastro que não deu certo.
 */
public class WebAuthnRegistrationRejectedException extends DomainException {
    public WebAuthnRegistrationRejectedException() {
        super("Não foi possível ativar a digital neste aparelho. Tente de novo.", HttpStatus.BAD_REQUEST);
    }
}
