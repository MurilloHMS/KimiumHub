package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Recusa a exclusão de um cadastro que ainda está sendo usado.
 *
 * **A mensagem chega inteira na tela.** O front-end mostra
 * `err.error.message` verbatim, e só cai na tabela genérica de códigos quando
 * o servidor não manda frase nenhuma. Então o texto passado aqui precisa
 * dizer QUEM está usando — é a única informação que desbloqueia quem clicou.
 *
 * É 409 e não 400: o pedido está bem formado, o estado é que não permite.
 */
public class CadastroEmUsoException extends DomainException {

    public CadastroEmUsoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
