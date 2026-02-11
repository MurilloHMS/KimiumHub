package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MachineAlreadyExistsException extends DomainException {
    public MachineAlreadyExistsException() {
        super("Máquina já está cadastrada!", HttpStatus.CONFLICT);
    }

}
