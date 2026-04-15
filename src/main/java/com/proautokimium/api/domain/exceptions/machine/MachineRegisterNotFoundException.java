package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MachineRegisterNotFoundException extends DomainException {
    public MachineRegisterNotFoundException() {
        super("O Registro da máquina não foi encontrado", HttpStatus.NOT_FOUND);
    }
}
