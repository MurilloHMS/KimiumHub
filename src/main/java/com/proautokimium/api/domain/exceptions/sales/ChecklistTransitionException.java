package com.proautokimium.api.domain.exceptions.sales;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * A ação não cabe na situação do checklist: aprovar o que já foi aprovado,
 * editar o que está aguardando análise. 409: o pedido é válido, o momento não.
 */
public class ChecklistTransitionException extends DomainException {
    public ChecklistTransitionException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
