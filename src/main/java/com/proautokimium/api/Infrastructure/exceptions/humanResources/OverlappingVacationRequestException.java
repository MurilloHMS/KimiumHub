package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class OverlappingVacationRequestException extends DomainException {
    public OverlappingVacationRequestException() {
        super("Já existe outro funcionário do mesmo setor de férias marcadas nesse período", HttpStatus.CONFLICT);
    }
}
