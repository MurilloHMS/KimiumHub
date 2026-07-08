package com.proautokimium.api.domain.exceptions.partners;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmployeeNotFoundException extends DomainException {
    public EmployeeNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public EmployeeNotFoundException(){
        super("Funcionário não encontrado", HttpStatus.NOT_FOUND);
    }
}
