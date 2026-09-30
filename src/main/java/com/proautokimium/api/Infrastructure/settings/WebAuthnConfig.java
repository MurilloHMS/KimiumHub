package com.proautokimium.api.Infrastructure.settings;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A webauthn4j como beans.
 *
 * <p>"Non-strict" porque pedimos {@code attestation: none}: não queremos saber a
 * marca do leitor, só a chave pública. O modo estrito exige conferir a cadeia
 * de certificados do fabricante, que aqui não existe.
 */
@Configuration
public class WebAuthnConfig {

    @Bean
    public ObjectConverter webAuthnObjectConverter() {
        return new ObjectConverter();
    }

    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter webAuthnObjectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(webAuthnObjectConverter);
    }

    /** Grava e lê de volta a chave pública da credencial (a coluna {@code attested_credential_data}). */
    @Bean
    public AttestedCredentialDataConverter attestedCredentialDataConverter(ObjectConverter webAuthnObjectConverter) {
        return new AttestedCredentialDataConverter(webAuthnObjectConverter);
    }
}
