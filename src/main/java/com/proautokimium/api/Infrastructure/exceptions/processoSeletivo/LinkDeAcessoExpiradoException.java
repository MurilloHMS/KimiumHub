package com.proautokimium.api.Infrastructure.exceptions.processoSeletivo;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Token expirado ou revogado: <b>410 Gone</b>.
 *
 * <p>410 e não 404 porque o recurso existiu — é o mesmo código que o
 * {@code ViewSecretsComponent} já trata no site, com a frase "já foi utilizado
 * ou expirou".
 *
 * <p>Revogado cai aqui junto com expirado: pedir um link novo mata os
 * anteriores, e quem clicar no e-mail antigo precisa ouvir a mesma coisa —
 * peça outro.
 */
public class LinkDeAcessoExpiradoException extends DomainException {
    public LinkDeAcessoExpiradoException() {
        super("Este link expirou ou foi substituído por um mais recente. Peça um novo.",
                HttpStatus.GONE);
    }
}
