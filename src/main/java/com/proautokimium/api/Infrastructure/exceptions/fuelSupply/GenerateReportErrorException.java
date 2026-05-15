package com.proautokimium.api.Infrastructure.exceptions.fuelSupply;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class GenerateReportErrorException extends DomainException {
    public GenerateReportErrorException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
