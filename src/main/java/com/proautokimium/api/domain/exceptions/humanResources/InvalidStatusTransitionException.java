package com.proautokimium.api.domain.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * A ação não cabe no estado atual do registro — aprovar o que já foi aprovado,
 * devolver o que já voltou. 409: o pedido é válido, o momento é que não.
 *
 * Substitui `IllegalStateException` nas entidades de RH, que caía no handler
 * genérico e virava 500 "Erro interno" — um clique duplo em "Aprovar" parecia
 * o servidor quebrando.
 */
public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
