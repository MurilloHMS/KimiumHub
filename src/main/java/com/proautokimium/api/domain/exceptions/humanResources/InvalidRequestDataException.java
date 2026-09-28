package com.proautokimium.api.domain.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O dado enviado não serve — data final antes da inicial, motivo em branco,
 * valor zero. 400, e a mensagem vai para a tela: quem enviou pode corrigir.
 *
 * Substitui `IllegalArgumentException` nas entidades de RH, que virava 500.
 */
public class InvalidRequestDataException extends DomainException {
    public InvalidRequestDataException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
