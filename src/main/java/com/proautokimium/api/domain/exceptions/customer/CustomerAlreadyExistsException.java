package com.proautokimium.api.domain.exceptions.customer;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CustomerAlreadyExistsException extends DomainException {
    public CustomerAlreadyExistsException() {
        super("Cliente já existe no banco");
    }

    @Override
    public HttpStatus getStatus(){
        return HttpStatus.CONFLICT;
    }
}
