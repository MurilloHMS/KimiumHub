package com.proautokimium.api.Infrastructure.exceptions.holerite;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/** Tipo de holerite que não existe, está desativado ou tem nome inválido. */
public class InvalidPayslipTypeException extends DomainException {
    public InvalidPayslipTypeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
