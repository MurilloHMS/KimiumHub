package com.proautokimium.api.domain.exceptions.customer;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends DomainException {
    public CustomerNotFoundException() {
        super("Cliente não encontrado");
    }

    @Override
    public HttpStatus getStatus() {return HttpStatus.NOT_FOUND; }
}
