package com.proautokimium.api.Infrastructure.exceptions.events;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * As recusas dos eventos. Mensagens em português porque são mostradas na tela
 * como vieram.
 */
public final class EventExceptions {

    private EventExceptions() {
    }

    public static class EventNotFoundException extends DomainException {
        public EventNotFoundException() {
            super("Evento não encontrado.", HttpStatus.NOT_FOUND);
        }
    }

    public static class TalkNotFoundException extends DomainException {
        public TalkNotFoundException() {
            super("Palestra não encontrada.", HttpStatus.NOT_FOUND);
        }
    }

    public static class SpeakerNotFoundException extends DomainException {
        public SpeakerNotFoundException() {
            super("Palestrante não encontrado.", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Dado que não fecha: período invertido, horário ao contrário, local incompleto.
     */
    public static class InvalidEventDataException extends DomainException {
        public InvalidEventDataException(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * O pedido faz sentido sozinho, mas quebra algo que já existe.
     */
    public static class EventConflictException extends DomainException {
        public EventConflictException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    /**
     * O evento já começou: a resposta fica como está.
     */
    public static class InvitationClosedException extends DomainException {
        public InvitationClosedException() { super("O evento já começou: a resposta não pode mais ser alterada.", HttpStatus.CONFLICT);
        }

        public InvitationClosedException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }
}
