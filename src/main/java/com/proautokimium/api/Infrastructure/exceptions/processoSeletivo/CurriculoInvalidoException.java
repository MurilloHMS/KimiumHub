package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O currículo enviado foi recusado.
 *
 * <p>É {@code DomainException} e não {@code InfrastructureException} porque
 * quem enviou <b>tem o que fazer a respeito</b>: trocar o arquivo. A mensagem
 * chega na tela, e é por isso que ela está em português e não cita caminho de
 * disco nem nome interno.
 */
public class CurriculoInvalidoException extends DomainException {

    private CurriculoInvalidoException(String message, HttpStatus status) {
        super(message, status);
    }

    public static CurriculoInvalidoException recusado(String motivo) {
        return new CurriculoInvalidoException(motivo, HttpStatus.BAD_REQUEST);
    }

    /**
     * 413, e não 400: o arquivo está correto, só é grande demais. A diferença
     * importa para quem lê o log e para quem escreve a mensagem na tela.
     */
    public static CurriculoInvalidoException grandeDemais(long tamanhoMaximoEmBytes) {
        long megabytes = tamanhoMaximoEmBytes / (1024 * 1024);
        return new CurriculoInvalidoException(
                "O currículo deve ter no máximo " + megabytes + " MB.",
                HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
