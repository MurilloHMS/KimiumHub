package com.proautokimium.api.Infrastructure.exceptions.machine;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

public class MachineContractErrorException extends InfrastructureException {
    public MachineContractErrorException(String message) {
        super(message);
    }
}
