package com.proautokimium.api.domain.exceptions.customer;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends DomainException {
    public CustomerNotFoundException() {
        super("Cliente não encontrado", HttpStatus.NOT_FOUND);
    }
}
