package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MachineMovementNotFoundException extends DomainException {
    public MachineMovementNotFoundException() {
        super("Movimento da máquina não encontrado", HttpStatus.NOT_FOUND);
    }

}
