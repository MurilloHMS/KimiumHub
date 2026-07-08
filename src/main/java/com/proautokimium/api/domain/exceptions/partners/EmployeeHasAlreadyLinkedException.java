package com.proautokimium.api.domain.exceptions.partners;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmployeeHasAlreadyLinkedException extends DomainException {
    public EmployeeHasAlreadyLinkedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
