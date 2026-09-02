package com.proautokimium.api.domain.exceptions.file;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O arquivo enviado não é uma imagem que o Java consiga decodificar.
 *
 * É `DomainException`, e não `InfrastructureException`, porque o
 * `DomainExceptionHandler` só conhece a primeira: como infra, isto chegava ao
 * designer como 500 — "erro interno do servidor" para um PDF que ele mandou
 * sem querer. Quem errou foi quem enviou, então é 400.
 */
public class FileNotImageException extends DomainException {

    public FileNotImageException() {
        super("O arquivo enviado não é uma imagem.", HttpStatus.BAD_REQUEST);
    }
}
