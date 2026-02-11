package com.proautokimium.api.domain.exceptions.certificate;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CertificateAlreadyExistsException extends DomainException {
    public CertificateAlreadyExistsException() {
        super("Já existe uma emissão de certificado no email cadastrado!", HttpStatus.CONFLICT);
    }
}
