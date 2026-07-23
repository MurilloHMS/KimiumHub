package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class VacationRequestNotFoundException extends DomainException {
    public VacationRequestNotFoundException() {
        super("Solicitação de férias não encontrada", HttpStatus.NOT_FOUND);
    }
}
