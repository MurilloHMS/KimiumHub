package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Token que não existe: <b>404</b>.
 *
 * <p>Distinguir isto de "expirou" (410) não vaza nada: para chegar a este
 * endpoint você já precisa ter um token na mão. E a diferença muda a frase que
 * a pessoa lê — "link inválido" manda conferir o endereço, "link expirado"
 * manda pedir outro.
 */
public class LinkDeAcessoInvalidoException extends DomainException {
    public LinkDeAcessoInvalidoException() {
        super("Link inválido. Confira o endereço ou peça um novo.", HttpStatus.NOT_FOUND);
    }
}
