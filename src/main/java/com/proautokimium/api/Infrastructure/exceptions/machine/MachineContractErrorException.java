package com.proautokimium.api.Infrastructure.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

public class MachineContractErrorException extends DomainException {
    public MachineContractErrorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
