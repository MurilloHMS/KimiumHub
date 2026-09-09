package com.proautokimium.api.Infrastructure.exceptions.newsletter;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O mês já foi fechado.
 *
 * **409, e não 400.** A requisição está certa; o que impede é o estado — e é
 * essa diferença que a tela usa para mostrar "este mês já foi confirmado" em vez
 * de um erro.
 *
 * A mensagem carrega quando foi e com quantos clientes, porque é o que a pessoa
 * precisa para decidir o que fazer. Um texto genérico apagaria isso.
 */
public class NewsletterMesJaConfirmadoException extends DomainException {

    public NewsletterMesJaConfirmadoException(String mensagem) {
        super(mensagem, HttpStatus.CONFLICT);
    }
}
