package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MachineNotFoundException extends DomainException {
    public MachineNotFoundException() {
        super("Máquina não foi encontrada!", HttpStatus.NOT_FOUND);
    }
}
