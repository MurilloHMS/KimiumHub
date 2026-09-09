package com.proautokimium.api.domain.exceptions.partners;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O código não existe no Sankhya.
 *
 * <p>404 e não 400: o pedido está bem formado, e o que falta é o parceiro. A
 * mensagem carrega o código porque quem digitou precisa saber qual foi — pode
 * ter sido um dígito trocado.
 */
public class ErpPartnerNotFoundException extends DomainException {

    public ErpPartnerNotFoundException(int codParceiro) {
        super("Nenhum parceiro com o código " + codParceiro + " no Sankhya.", HttpStatus.NOT_FOUND);
    }
}
