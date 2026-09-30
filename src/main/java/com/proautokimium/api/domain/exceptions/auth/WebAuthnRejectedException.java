package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * A digital não confirmou — e o motivo não é dito.
 *
 * <p>Credencial desconhecida, assinatura que não bate, origem errada, contador
 * que andou para trás: a mesma frase em todos, como o
 * {@link RefreshTokenInvalidoException}. Cada distinção seria uma dica de graça
 * para quem está testando. O motivo real vai para o log.
 *
 * <p>{@code 401}, o mesmo da senha errada: para a tela de login, os dois são
 * "não entrou, tente de novo".
 */
public class WebAuthnRejectedException extends DomainException {
    public WebAuthnRejectedException() {
        super("Não foi possível confirmar pela digital. Tente de novo, ou entre com a senha.", HttpStatus.UNAUTHORIZED);
    }
}
