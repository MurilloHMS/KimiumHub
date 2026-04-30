package com.proautokimium.api.Infrastructure.exceptions.product;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException() {
        super("Produto não encontrado", HttpStatus.NOT_FOUND);
    }
}
