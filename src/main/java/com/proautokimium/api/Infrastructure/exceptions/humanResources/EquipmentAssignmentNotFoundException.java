package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EquipmentAssignmentNotFoundException extends DomainException {
    public EquipmentAssignmentNotFoundException() {
        super("Registro de equipamento não encontrado", HttpStatus.NOT_FOUND);
    }
}
