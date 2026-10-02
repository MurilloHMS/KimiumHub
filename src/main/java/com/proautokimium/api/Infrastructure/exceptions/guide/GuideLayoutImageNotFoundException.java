package com.proautokimium.api.Infrastructure.exceptions.guide;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class GuideLayoutImageNotFoundException extends DomainException {
    public GuideLayoutImageNotFoundException() {
        super("Imagem do layout não encontrada", HttpStatus.NOT_FOUND);
    }
}
