package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MedicalCertificateNotFoundException extends DomainException {
    public MedicalCertificateNotFoundException() {
        super("Atestado não encontrado", HttpStatus.NOT_FOUND);
    }
}
