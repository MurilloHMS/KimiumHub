package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmployeePayrollDataMissingException extends DomainException {
    public EmployeePayrollDataMissingException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
