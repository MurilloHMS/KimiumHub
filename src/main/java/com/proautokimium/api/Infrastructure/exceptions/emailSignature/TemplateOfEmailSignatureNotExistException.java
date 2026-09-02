package com.proautokimium.api.Infrastructure.exceptions.emailSignature;

public class TemplateOfEmailSignatureNotExistException extends RuntimeException {
    public TemplateOfEmailSignatureNotExistException(String message) {
        super(message);
    }
    public TemplateOfEmailSignatureNotExistException() {
        super("Email Signature Not Exist");
    }
}
