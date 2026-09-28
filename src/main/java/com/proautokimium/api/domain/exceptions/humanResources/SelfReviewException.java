package com.proautokimium.api.domain.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Quem revisa um pedido não pode ser o dono dele — nem para aprovar, nem para
 * reprovar, nem no lançamento de férias do RH, que já nasce aprovado.
 *
 * 403: a pessoa tem a permissão da tela, mas não sobre este pedido. A saída é
 * outra pessoa com a mesma permissão — por isso a grade precisa de pelo menos
 * duas pessoas com a célula de aprovação.
 */
public class SelfReviewException extends DomainException {
    public SelfReviewException() {
        super("Você não pode revisar o seu próprio pedido. Outra pessoa com permissão precisa fazer isso.",
                HttpStatus.FORBIDDEN);
    }
}
