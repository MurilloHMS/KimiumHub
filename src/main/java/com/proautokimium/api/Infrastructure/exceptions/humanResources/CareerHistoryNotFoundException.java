package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CareerHistoryNotFoundException extends DomainException {
    public CareerHistoryNotFoundException() {
        super("Funcionário não tem histórico de carreira registrado", HttpStatus.CONFLICT);
    }
}
