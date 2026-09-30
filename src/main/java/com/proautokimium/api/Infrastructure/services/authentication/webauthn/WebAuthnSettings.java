package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.webauthn4j.data.client.Origin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O domínio a que as digitais ficam presas, e de onde podem ser pedidas.
 *
 * <p>Conferido na subida: uma origem sem {@code https://} só é aceita em
 * {@code localhost}. O navegador recusaria de qualquer jeito, mas a mensagem
 * dele chega ao funcionário como "não funcionou", e esta chega ao deploy.
 */
@Component
public class WebAuthnSettings {

    private final String rpId;
    private final String rpName;
    private final Set<Origin> origins;

    public WebAuthnSettings(@Value("${app.webauthn.rp-id}") String rpId,
                            @Value("${app.webauthn.rp-name}") String rpName,
                            @Value("${app.webauthn.origins}") String origins) {
        if (rpId == null || rpId.isBlank()) {
            throw new IllegalStateException("app.webauthn.rp-id vazio: defina WEBAUTHN_RP_ID.");
        }
        this.rpId = rpId.trim();
        this.rpName = rpName.trim();
        this.origins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(WebAuthnSettings::checked)
                .collect(Collectors.toUnmodifiableSet());
        if (this.origins.isEmpty()) {
            throw new IllegalStateException("app.webauthn.origins vazio: defina WEBAUTHN_ORIGINS.");
        }
    }

    private static Origin checked(String origin) {
        boolean local = origin.startsWith("http://localhost") || origin.startsWith("http://127.0.0.1");
        if (!origin.startsWith("https://") && !local) {
            throw new IllegalStateException("Origem do WebAuthn sem https: " + origin);
        }
        return new Origin(origin);
    }

    public String rpId() { return rpId; }

    public String rpName() { return rpName; }

    public Set<Origin> origins() { return origins; }
}
