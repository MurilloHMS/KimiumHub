package com.proautokimium.api.Infrastructure.exceptions.certificate;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;

public class FailedToCreateCertificate extends InfrastructureException {
    public FailedToCreateCertificate(String message, Throwable cause) { super(message,cause);}
    public FailedToCreateCertificate(String message) { super(message);}
}
