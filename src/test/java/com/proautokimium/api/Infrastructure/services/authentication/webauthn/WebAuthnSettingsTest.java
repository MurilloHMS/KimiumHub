package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.webauthn4j.data.client.Origin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A configuração é conferida na subida: erro de deploy aparece no deploy, não no celular de alguém. */
class WebAuthnSettingsTest {

    @Test
    @DisplayName("várias origens separadas por vírgula, com espaço em volta")
    void severalOrigins() {
        WebAuthnSettings s = new WebAuthnSettings("proautokimium.com.br", "KimiumHub",
                "https://proautokimium.com.br, https://www.proautokimium.com.br");

        assertThat(s.origins()).containsExactlyInAnyOrder(
                new Origin("https://proautokimium.com.br"), new Origin("https://www.proautokimium.com.br"));
    }

    @Test
    @DisplayName("http só vale em localhost")
    void httpOnlyOnLocalhost() {
        assertThat(new WebAuthnSettings("localhost", "KimiumHub", "http://localhost:4200").origins()).hasSize(1);
        assertThatThrownBy(() -> new WebAuthnSettings("proautokimium.com.br", "KimiumHub", "http://proautokimium.com.br"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("sem https");
    }

    @Test
    @DisplayName("sem domínio ou sem origem, a API não sobe")
    void emptyRefused() {
        assertThatThrownBy(() -> new WebAuthnSettings(" ", "KimiumHub", "https://proautokimium.com.br"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new WebAuthnSettings("proautokimium.com.br", "KimiumHub", " , "))
                .isInstanceOf(IllegalStateException.class);
    }
}
