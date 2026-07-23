package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class InsufficientVacationBalanceException extends DomainException {
    public InsufficientVacationBalanceException() {
        super("Saldo de férias insuficiente para o período solicitado", HttpStatus.CONFLICT);
    }
}
