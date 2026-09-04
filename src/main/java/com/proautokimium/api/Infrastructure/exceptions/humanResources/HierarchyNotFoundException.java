package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class HierarchyNotFoundException extends DomainException {
    public HierarchyNotFoundException() {
        super("Hierarquia não encontrada", HttpStatus.NOT_FOUND);
    }
}
