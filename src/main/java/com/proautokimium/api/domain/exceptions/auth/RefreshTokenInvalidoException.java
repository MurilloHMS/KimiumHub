package com.proautokimium.api.domain.exceptions.auth;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O refresh token não serve — e o motivo não é dito.
 *
 * Quatro situações caem aqui: não existe, venceu, já foi usado, foi revogado. A
 * mensagem é a mesma nas quatro de propósito. Dizer "este já foi usado" a quem
 * está testando tokens confirma que ele acertou um valor real, e "expirado"
 * confirma que existiu — cada distinção é uma dica de graça.
 *
 * Para quem tem a sessão legítima, os quatro casos têm a mesma saída: entrar de
 * novo.
 *
 * <p>{@code 401} e não {@code 403}: a diferença importa para o interceptor do
 * front, que trata {@code 401} como sessão caída e {@code 403} como "você não
 * pode isto". Um {@code 403} aqui deixaria a pessoa presa numa tela sem
 * explicação, em vez de levá-la ao login.
 */
public class RefreshTokenInvalidoException extends DomainException {
    public RefreshTokenInvalidoException() {
        super("Sessão expirada. Entre novamente.", HttpStatus.UNAUTHORIZED);
    }
}
